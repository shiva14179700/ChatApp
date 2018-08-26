package e.pavanmalisetti.androidchatapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBChatDialogParticipantListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.model.QBPresence;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.chat.request.QBMessageUpdateBuilder;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.Inflater;

import e.pavanmalisetti.androidchatapp.Adapter.ChatMessageAdapter;
import e.pavanmalisetti.androidchatapp.Common.Common;
import e.pavanmalisetti.androidchatapp.Holder.QBChatMessagesHolder;

public class ChatMessageActivity extends AppCompatActivity implements QBChatDialogMessageListener{

    QBChatDialog qbChatDialog;
    ListView lstChatMessages;
    ImageButton submitButton;
    EditText edtContent;

    ChatMessageAdapter adapter;

    //update online user
    ImageView img_online_count,dialog_avatar;
    TextView txt_online_count;

    //variables for update/delete message
    int ContextMenuIndexClicked=-1;
    boolean isEditMode=false;
    QBChatMessage editMessage;

    //Tool bar
    Toolbar toolbar;

    //dialog avatar
    static  final int SELECT_PICTURE=7171;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (qbChatDialog.getType()==QBDialogType.GROUP||qbChatDialog.getType()==QBDialogType.PUBLIC_GROUP){
            getMenuInflater().inflate(R.menu.chat_message_group_menu,menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.chat_group_edit_name:
                editGroupName();
                break;
            case R.id.chat_group_add_user:
                addUser();
                break;
            case R.id.chat_group_remove_user:
                removeUser();
                break;
            default:
                break;
        }
        return true;
    }

    private void removeUser() {

        Intent intent=new Intent(ChatMessageActivity.this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_REMOVE_MODE);
        startActivity(intent);
    }

    private void addUser() {

        Intent intent=new Intent(ChatMessageActivity.this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_ADD_MODE);
        startActivity(intent);

    }

    private void editGroupName() {
        LayoutInflater inflater=LayoutInflater.from(this);
        View view= inflater.inflate(R.layout.dialog_edit_group_layout,null);

        AlertDialog.Builder alertDialogBuilder=new AlertDialog.Builder(this);
        alertDialogBuilder.setView(view);
        final EditText newName=(EditText)view.findViewById(R.id.edt_new_group_name);

        //set dialog message
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        qbChatDialog.setName(newName.getText().toString()); //set new name for dialog object

                        QBDialogRequestBuilder requestBuilder=new QBDialogRequestBuilder();

                        QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                .performAsync(new QBEntityCallback<QBChatDialog>() {
                                    @Override
                                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                        Toast.makeText(ChatMessageActivity.this, "Group Name Edited successfully", Toast.LENGTH_SHORT).show();
                                        toolbar.setTitle(qbChatDialog.getName());
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        Toast.makeText(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        //create alert dialog
        AlertDialog alertDialog=alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //GET INDEX CONTEXT MENU CLICK
       AdapterView.AdapterContextMenuInfo info=(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
       ContextMenuIndexClicked=info.position;

        switch (item.getItemId()){
            case R.id.chat_message_update_message:
                updateMessage();
                break;
            case R.id.chat_message_delete_message:
                deleteMessage();
            default:
                break;
        }
        return true;
    }

    private void deleteMessage() {
        final ProgressDialog deletedialog=new ProgressDialog(ChatMessageActivity.this);
        deletedialog.setMessage("Please wait...");
        deletedialog.show();

        editMessage=QBChatMessagesHolder.getInstance().getChatMessagesByDialog(qbChatDialog.getDialogId())
                .get(ContextMenuIndexClicked);
        QBRestChatService.deleteMessage(editMessage.getId(),false).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                retrieveMessage();
                deletedialog.dismiss();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    private void updateMessage() {

        //set message for edit text
        editMessage=QBChatMessagesHolder.getInstance().getChatMessagesByDialog(qbChatDialog.getDialogId())
                .get(ContextMenuIndexClicked);
        edtContent.setText(editMessage.getBody());
        isEditMode=true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.chat_message_context_menu,menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);

        initViews();
        initChatDialogs();
        retrieveMessage();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!edtContent.getText().toString().isEmpty()){
                if (!isEditMode) {
                    QBChatMessage chatMessage = new QBChatMessage();
                    chatMessage.setBody(edtContent.getText().toString());
                    chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                    chatMessage.setSaveToHistory(true);

                    try {
                        qbChatDialog.sendMessage(chatMessage);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }

                    //Fix private chat don't show message
                    if (qbChatDialog.getType() == QBDialogType.PRIVATE) {
                        //save incoming message to cache
                        QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(), chatMessage);
                        ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialog(qbChatDialog.getDialogId());
                        adapter = new ChatMessageAdapter(getBaseContext(), messages);
                        lstChatMessages.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }

                    //remove text from edit text
                    edtContent.setText("");
                    edtContent.setFocusable(true);
                }else{
                    final ProgressDialog updatedialog=new ProgressDialog(ChatMessageActivity.this);
                    updatedialog.setMessage("Please wait...");
                    updatedialog.show();

                    QBMessageUpdateBuilder messageUpdateBuilder=new QBMessageUpdateBuilder();
                    messageUpdateBuilder.updateText(edtContent.getText().toString()).markDelivered().markRead();

                    QBRestChatService.updateMessage(editMessage.getId(),qbChatDialog.getDialogId(),messageUpdateBuilder)
                            .performAsync(new QBEntityCallback<Void>() {
                                @Override
                                public void onSuccess(Void aVoid, Bundle bundle) {
                                    //refresh data
                                    retrieveMessage();
                                    isEditMode=false;
                                    updatedialog.dismiss();

                                    //remove text from edit text
                                    edtContent.setText("");
                                    edtContent.setFocusable(true);
                                }

                                @Override
                                public void onError(QBResponseException e) {
                                    Toast.makeText(getBaseContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                }
            }
            }
        });

    }

    private void retrieveMessage() {
        QBMessageGetBuilder messageGetBuilder=new QBMessageGetBuilder();
        messageGetBuilder.setLimit(500); //get limit 500 messages

        if (qbChatDialog!=null){
            QBRestChatService.getDialogMessages(qbChatDialog,messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                    //save list messages to cache and refresh list view
                    QBChatMessagesHolder.getInstance().putMessages(qbChatDialog.getDialogId(),qbChatMessages);

                    adapter=new ChatMessageAdapter(getBaseContext(),qbChatMessages);
                    lstChatMessages.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        }
    }

    private void initChatDialogs()  {

        qbChatDialog=(QBChatDialog)getIntent().getSerializableExtra(Common.DIALOG_EXTRA);

        if (qbChatDialog.getPhoto()!=null){
            QBContent.getFile(Integer.parseInt(qbChatDialog.getPhoto()))
                    .performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            String fileURL=qbFile.getPrivateUrl();
                            Picasso.with(getBaseContext())
                                    .load(fileURL)
                                    .resize(50,50)
                                    .centerCrop()
                                    .into(dialog_avatar);
                        }

                        @Override
                        public void onError(QBResponseException e) {
                           Log.e("ERROR_IMAGE",""+e.getMessage());
                        }
                    });
        }


        qbChatDialog.initForChat(QBChatService.getInstance());

        //register listener incoming message
        QBIncomingMessagesManager incomingMessage=QBChatService.getInstance().getIncomingMessagesManager();
        incomingMessage.addDialogMessageListener(new QBChatDialogMessageListener() {
            @Override
            public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {

            }

            @Override
            public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

            }
        });

        //Add Join Group to enable group chat
        if(qbChatDialog.getType()== QBDialogType.PUBLIC_GROUP || qbChatDialog.getType()==QBDialogType.GROUP){
            DiscussionHistory discussionHistory=new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);

            qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {

                }

                @Override
                public void onError(QBResponseException e) {
                  Log.d("ERROR",""+e.getMessage());
                }
            });

        }

        QBChatDialogParticipantListener participantListener=new QBChatDialogParticipantListener() {
            @Override
            public void processPresence(String dialogId, QBPresence qbPresence) {
                   if (dialogId==qbChatDialog.getDialogId()){
                       QBRestChatService.getChatDialogById(dialogId)
                               .performAsync(new QBEntityCallback<QBChatDialog>() {
                                   @Override
                                   public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                       //get online user
                                       try {
                                           Collection<Integer> onlineList=qbChatDialog.getOnlineUsers();
                                           TextDrawable.IBuilder builder=TextDrawable.builder()
                                                   .beginConfig()
                                                   .withBorder(4)
                                                   .endConfig()
                                                   .round();
                                           TextDrawable online=builder.build("", Color.RED);
                                           img_online_count.setImageDrawable(online);

                                           txt_online_count.setText(String.format("%d/%d online",onlineList.size(),qbChatDialog.getOccupants().size()));
                                       } catch (XMPPException e) {
                                           e.printStackTrace();
                                       }
                                   }

                                   @Override
                                   public void onError(QBResponseException e) {

                                   }
                               });
                   }
            }
        };

        qbChatDialog.addParticipantListener(participantListener);

        qbChatDialog.addMessageListener(this);

        //set title for toolbar
        toolbar.setTitle(qbChatDialog.getName());
        setSupportActionBar(toolbar);
    }

    private void initViews() {

        lstChatMessages=(ListView)findViewById(R.id.list_of_message);
        submitButton=(ImageButton)findViewById(R.id.send_button);
        edtContent=(EditText)findViewById(R.id.edt_content);

        img_online_count=(ImageView)findViewById(R.id.img_online_count);
        txt_online_count=(TextView)findViewById(R.id.txt_online_count);

        //dialog avatar-profile pic
        dialog_avatar=(ImageView)findViewById(R.id.dialog_avatar);
        dialog_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectImage=new Intent();
                selectImage.setType("image/*");
                selectImage.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectImage,"Select picture"),SELECT_PICTURE);
            }
        });

        //Add context Menu
        registerForContextMenu(lstChatMessages);

        //Add Tool Bar
        toolbar=(Toolbar)findViewById(R.id.chat_message_toolbar);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK){
            if (requestCode==SELECT_PICTURE){
                Uri selectedImageUri=data.getData();
                final ProgressDialog mDialog=new ProgressDialog(ChatMessageActivity.this);
                mDialog.setMessage("Please wait..");
                mDialog.setCancelable(false);
                mDialog.show();

                try{
                    //convert uri to file
                    InputStream in=getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap= BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos=new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                    File file=new File(Environment.getExternalStorageDirectory()+"/image.png");
                    FileOutputStream fileOut=new FileOutputStream(file);
                    fileOut.write(bos.toByteArray());
                    fileOut.flush();
                    fileOut.close();

                    int imageSizekb=(int)file.length()/1024;
                    if (imageSizekb>=(1024*100)){
                        Toast.makeText(this, "Error size", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Upload file
                    QBContent.uploadFileTask(file,true,null)
                            .performAsync(new QBEntityCallback<QBFile>() {
                                @Override
                                public void onSuccess(QBFile qbFile, Bundle bundle) {
                                    //if uploading becomes success we will make that pic as profile pic for chat dialog
                                    qbChatDialog.setPhoto(qbFile.getId().toString());

                                    //update chat dialog
                                    QBRequestUpdateBuilder requestBuilder=new QBRequestUpdateBuilder();
                                    QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                            .performAsync(new QBEntityCallback<QBChatDialog>() {
                                                @Override
                                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                                    mDialog.dismiss();
                                                    dialog_avatar.setImageBitmap(bitmap);
                                                }

                                                @Override
                                                public void onError(QBResponseException e) {
                                                    Toast.makeText(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        //save incoming message to cache
        QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(),qbChatMessage);
        ArrayList<QBChatMessage> messages=QBChatMessagesHolder.getInstance().getChatMessagesByDialog(qbChatDialog.getDialogId());
        adapter=new ChatMessageAdapter(getBaseContext(),messages);
        lstChatMessages.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {
       Log.e("ERROR",""+e.getMessage());
    }
}
