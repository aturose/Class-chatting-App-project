package com.example.chat.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.AddPostActivity;
import com.example.chat.PostLikedByActivity;
import com.example.chat.PostDetailActivity;
import com.example.chatapp.R;
import com.example.chat.ThereProfileActivity;
import com.example.chat.models.ModelPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;


import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder>{

    Context context;
    List<ModelPost> postList;

    String myUid;

    private DatabaseReference likesRef; //for likes database node
    private DatabaseReference postsRef; //reference of posts

    boolean mProcessLike=false;
    private String pDescription;


    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");


    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout row_post.xml
        View view  = LayoutInflater.from(context).inflate(R.layout.row_posts, parent,false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {
        //get data
        final String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        final String uDp = postList.get(position).getuDp();
        final String pId = postList.get(position).getpId();
        //final String pTitle = postList.get(position).getpTitle();
        final String pDescription = postList.get(position).getpDescr();
        final String pImage = postList.get(position).getpImage();
        String pTimeStamp = postList.get(position).getpTime();
        String pLikes = postList.get(position).getpLikes(); //contains total number of likes for a post
        String pComments = postList.get(position).getpComments(); //contains total number of comments for a post

        //convert timestamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        final String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        //set data
        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(pTime);
        //holder.pTitleTv.setText(pTitle);
        holder.pDescriptionTv.setText(pDescription);
        holder.pLikesTv.setText(pLikes +" Likes"); //e.g. 100 Likes
        holder.pCommentsTv.setText(pComments +" Comments"); //e.g. 100 comments
        //set likes for each post
        setLikes(holder, pId);

        //set user dp
        try {
            Picasso.get().load(uDp).placeholder(R.drawable.ic_default_img).into(holder.uPictureIv);
        }
        catch (Exception e){

        }

        //set post image
        //if there is no image i.e. pImage.equals("noImage") then hide ImageView
        if (pImage.equals("noImage")){
            //hide imageview
            holder.pImageIv.setVisibility(View.GONE);
        }
        else {
            //show imageview
            holder.pImageIv.setVisibility(View.VISIBLE); // make sure to collect this

            try {
                Picasso.get().load(pImage).into(holder.pImageIv);
            }
            catch (Exception e){

            }
        }

        //handle button clicks
        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions(holder.moreBtn, uid, myUid, pId, pImage);
            }
        });
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get total number of likes for the post, whose like button clicked
                //if currently signed in user has not liked it before
                //increase value by 1, otherwise decrease value by 1
                final int pLikes = Integer.parseInt(postList.get(position).getpLikes());
                mProcessLike = true;
                //get id of the post clicked
                final String postIde = postList.get(position).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mProcessLike){
                            if (snapshot.child(postIde).hasChild(myUid)){
                                //already liked, so remove like
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes-1));
                                likesRef.child(postIde).child(myUid).removeValue();
                                mProcessLike = false;
                            }
                            else {
                                // not liked, like it
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes+1));
                                likesRef.child(postIde).child(myUid).setValue("Liked"); //set any value
                                mProcessLike = false;

                                addToHisNotifications(""+uid,""+pId,"Liked your post");

                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
        holder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start PostDetailActivity
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", pId); //will get detail of the post using this Id, its id of the pos clicked
                context.startActivity(intent);

            }
        });
        holder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*some posts contains only text, and some contains image and text so
                we will handle them both*/
                //get image from imageview
                BitmapDrawable bitmapDrawable = (BitmapDrawable)holder.pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    //post without image
                    shareTextOnly(pDescription);

                }
                else {
                    //post with image

                    //convert image to bitmap
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText( pDescription, bitmap);

                }
            }
        });
        holder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*click to go to ThereProfileActivity with uid, this uid is of clicked user
                * which will be used to show user specific data/posts*/
                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid",uid);
                context.startActivity(intent);
            }
        });

        //click like count to start PostLikedByActivity, and pass the popst id
        holder.pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PostLikedByActivity.class);
                intent.putExtra("postId", pId);
                context.startActivity(intent);
            }
        });

    }

    private void addToHisNotifications(String hisUid, String pId, String notification){
        //timestamp for time and notification id
        String timestamp = ""+System.currentTimeMillis();

        //data to put in notification in firebase
        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added successfully

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed
                    }
                });
    }

    private void shareTextOnly(String pTitle) {
        //concatenate title and description to share
        String shareBody = pTitle +"\n"+ pDescription;

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Enter your subject here"); //in case you share via an email app
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody); //text to share
        context.startActivity(Intent.createChooser(sIntent, "Share Via")); //message to show in share dialog

    }

    private void shareImageAndText(String pTitle, Bitmap bitmap) {
        //concatenate title and description to share
        String shareBody = pTitle +"\n"+ pDescription;

        //first we will save this in cache, get the saved image uri
        Uri uri = saveImageToShare(bitmap);

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Enter your subject here");
        sIntent.setType("image/png");
        context.startActivity(Intent.createChooser(sIntent,"Share Via"));

    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(),"images");
        Uri uri = null;
        try {
            imageFolder.mkdir(); //create if not exists
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context,"com.example.sseseguide.fileprovider",
                    file);

        }
        catch (Exception e){
            Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void setLikes(final MyHolder holder, final String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postKey).hasChild(myUid)){
                    //user has liked this post
                    /*To indicate that the post is liked by this (SignedIn) user
                    Change drawable left icon of the like button
                    Change text of like button from "Like" to "Liked"*/
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0,0,0);
                    holder.likeBtn.setText("Liked");

                }
                else {
                    //user has not liked this post
                     /*To indicate that the post is not liked by this (SignedIn) user
                    Change drawable left icon of the like button
                    Change text of like button from "Liked" to "Like"*/
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0,0,0);
                    holder.likeBtn.setText("Like");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, final String pId, final String pImage) {
        //creating popup menu currently having option Delete, we will add more options later
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        //show delete option in only post(s) of currently signed-in user
        if (uid.equals(myUid)){
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete post");
            popupMenu.getMenu().add(Menu.NONE,1,0,"Edit post");
        }
        popupMenu.getMenu().add(Menu.NONE,2,0,"See Post Details");


        //item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id==0){
                    //delete is clicked
                    beginDelete(pId, pImage);
                }
                else if (id==1){
                    //Edit is clicked
                    //start AddPostActivity with key "editPost" and the id of the post clicked
                    Intent intent = new Intent(context, AddPostActivity.class);
                    intent.putExtra("key","editPost");
                    intent.putExtra("editPostId",pId);
                    context.startActivity(intent);
                }
                else if (id==2){
                    //start PostDetailActivity
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId", pId); //will get detail of the post using this Id, its id of the pos clicked
                    context.startActivity(intent);
                }
                return false;
            }
        });
        //show menu
        popupMenu.show();
    }

    private void beginDelete(String pId, String pImage) {
        //post can be withor without image
        if (pImage.equals("noImage")){
            //post is without image
            deleteWithoutImage(pId);
        }
        else {
            //post is with image
            deleteWithImage(pId, pImage);
        }
    }

    private void deleteWithImage(final String pId, String pImage) {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting post,Please wait...");
        /*Steps:
        * 1) Delete Image using url
        * 2) Delete from database using post id*/

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image deleted, now delete database

                        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds: snapshot.getChildren()){
                                    ds.getRef().removeValue(); // remove values from firebase where pid matches
                                }
                                //deleted
                                Toast.makeText(context, "Post deleted successfully!!!", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed, can't go further
                        pd.dismiss();
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void deleteWithoutImage(String pId) {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting post,Please wait...");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    ds.getRef().removeValue(); // remove values from firebase where pid matches
                }
                //deleted
                Toast.makeText(context, "Post deleted successfully!!!", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views from row_post.xml
        ImageView uPictureIv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn, shareBtn;
        LinearLayout profileLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            uPictureIv = itemView.findViewById(R.id.uPictureIv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            profileLayout = itemView.findViewById(R.id.profileLayout);
        }
    }
}
