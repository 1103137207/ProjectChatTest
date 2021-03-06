package com.example.no24519.projectchat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView mProfileName,mProfileStatus,mProfileFreindCount;
    private TextView mIntroductionStatus;
    private TextView mEmotionalStatus;
    private TextView mInterestStatus;
    private TextView mCityStatus;
    private ImageView mProfileImage;
    private Button mProfileSendReqBtn,mDeclineBtn;

    private DatabaseReference mUsersDateBase;

    private ProgressDialog mProgress;

    private String mCurrent_state;

    private FirebaseUser mCurrent_user;

    private DatabaseReference mFriendReqDataBase;
    private DatabaseReference mFriendDateBase;
    private DatabaseReference mNotificationDataBase;
    private  DatabaseReference mRootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");

        mUsersDateBase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendReqDataBase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDateBase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDataBase = FirebaseDatabase.getInstance().getReference("Notifications");

        mRootRef = FirebaseDatabase.getInstance().getReference();

        mCurrent_user = FirebaseAuth.getInstance().getCurrentUser();


        mProfileImage = (ImageView)findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_displayName);
        mProfileStatus = (TextView) findViewById(R.id.profile_status);
        mCityStatus = (TextView) findViewById(R.id.profile_city_status);
        mInterestStatus = (TextView) findViewById(R.id.profile_interest_status);
        mEmotionalStatus = (TextView) findViewById(R.id.profile_emotional_status);
        mIntroductionStatus = (TextView) findViewById(R.id.profile_introduction_status);
        mProfileFreindCount = (TextView) findViewById(R.id.profile_totalFriends);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_send_req_btn);
        mDeclineBtn = (Button) findViewById(R.id.profile_decline_btn);

        mCurrent_state = "not_friends";

        mDeclineBtn.setVisibility(View.INVISIBLE);
        mDeclineBtn.setEnabled(false);

        mProgress = new ProgressDialog(this);
        mProgress.setTitle("載入資料中");
        mProgress.setMessage("載入對方資料中.請稍等");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mUsersDateBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String city = dataSnapshot.child("city").getValue().toString();
                String interest = dataSnapshot.child("interest").getValue().toString();
                String emotional = dataSnapshot.child("emotional").getValue().toString();
                String introduction = dataSnapshot.child("introduction").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);
                mCityStatus.setText("居住地"+city);
                mInterestStatus.setText("興趣／專長："+interest);
                mEmotionalStatus.setText("感情狀況："+emotional);
                mIntroductionStatus.setText("自我介紹："+introduction);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mProfileImage);

                // ------------------------好友清單---------------------------------------------------

                mFriendReqDataBase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(user_id)){

                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if (req_type.equals("received")){

                                mCurrent_state = "req_received";
                                mProfileSendReqBtn.setText("接受好友邀請");

                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);

                            }else if (req_type.equals("sent")){

                                mCurrent_state = "req_sent";
                                mProfileSendReqBtn.setText("取消交友邀請");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);

                            }
                            mProgress.dismiss();

                        }
                        else {
                            mFriendDateBase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(user_id)){

                                        mCurrent_state = "friends";
                                        mProfileSendReqBtn.setText("刪除好友");

                                        mDeclineBtn.setVisibility(View.INVISIBLE);
                                        mDeclineBtn.setEnabled(false);

                                    }

                                    mProgress.dismiss();

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                    mProgress.dismiss();

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                        mProgress.dismiss();
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProfileSendReqBtn.setEnabled(false);

                //------------------------未成為好友-----------------------------------
                if (mCurrent_state.equals("not_friends")){

                    DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_id).push();

                    String newNotificationId = newNotificationref.getKey();

                    HashMap<String,String> notificationData = new HashMap<String, String>();
                    notificationData.put("from",mCurrent_user.getUid());
                    notificationData.put("type","request");

                    Map requestMap =new HashMap();
                    requestMap.put( "Friend_req/" + mCurrent_user.getUid() + "/"+user_id+"/request_type","sent");
                    requestMap.put( "Friend_req/" + user_id+ "/"+mCurrent_user.getUid()+"/request_type","received");
                    requestMap.put( "notifications/" + user_id + "/"+newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError != null){

                                Toast.makeText(ProfileActivity.this,"There was some error in sending request",Toast.LENGTH_LONG).show();

                            }
                            mProfileSendReqBtn.setEnabled(true);

                            mCurrent_state = "req_sent";
                            mProfileSendReqBtn.setText("取消交友邀請");

                        }
                    });

                }
                // ----------------------------取消好友邀請---------------------------------------
                if (mCurrent_state.equals("req_sent")){
                    mFriendReqDataBase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqDataBase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    mProfileSendReqBtn.setText("送出好友邀請");

                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);
                                }
                            });
                        }
                    });
                }

                //------------------------------------REQ RECEIVED STATE--------------------

                if (mCurrent_state.equals("req_received")){

                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put("Friends/"+mCurrent_user.getUid()+"/"+user_id+"/date",currentDate);
                    friendsMap.put("Friends/"+user_id+"/"+mCurrent_user.getUid()+"/date",currentDate);

                    friendsMap.put("Friend_req/"+mCurrent_user.getUid()+"/"+user_id,null);
                    friendsMap.put("Friend_req/"+user_id+"/"+mCurrent_user.getUid(),null);

                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null){

                                mProfileSendReqBtn.setEnabled(true);
                                mCurrent_state = "friends";
                                mProfileSendReqBtn.setText("刪除好友");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);

                            }else {

                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this,error,Toast.LENGTH_LONG).show();

                            }

                        }
                    });


                }

                //--------------------UNFRIEND---------------------
                if (mCurrent_state.equals("friends")){
                    CharSequence options[] = new CharSequence[]{"查看好友資訊","好友聊天"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                    builder.setTitle("警告");
                    builder.setMessage("確定要刪除好友嗎?");
                    builder.setPositiveButton("確認", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {


                        Map unfriendsMap = new HashMap();

                        unfriendsMap.put("Friends/"+mCurrent_user.getUid()+"/"+user_id,null);
                        unfriendsMap.put("Friends/"+user_id+"/"+mCurrent_user.getUid(),null);

                        mRootRef.updateChildren(unfriendsMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                if (databaseError == null){

                                    mCurrent_state = "not_friends";
                                    mProfileSendReqBtn.setText("送出好友邀請");

                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);

                                    Intent startIntent = new Intent(ProfileActivity.this, FriendsActivity.class);
                                    startActivity(startIntent);
                                    finish();

                                }else {

                                    String error = databaseError.getMessage();
                                    Toast.makeText(ProfileActivity.this,error,Toast.LENGTH_LONG).show();

                                }

                            }
                        });
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(ProfileActivity.this, "取消",Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.show();
                }

            }
        });
    }
}
