package e.pavanmalisetti.androidchatapp;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.utils.DialogUtils;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;

import java.util.ArrayList;
import java.util.List;

import e.pavanmalisetti.androidchatapp.Adapter.ListUsersAdapter;
import e.pavanmalisetti.androidchatapp.Common.Common;
import e.pavanmalisetti.androidchatapp.Holder.QBUsersHolder;

public class ListUsersActivity extends AppCompatActivity {


    ListView lstUsers;
    Button btnCreateChat;

    String mode="";
    QBChatDialog qbChatDialog;
    List<QBUser> userAdd=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_users);

        mode=getIntent().getStringExtra(Common.UPDATE_MODE);
        qbChatDialog=(QBChatDialog)getIntent().getSerializableExtra(Common.UPDATE_DIALOG_EXTRA);

        lstUsers=(ListView)findViewById(R.id.lstUsers);
        lstUsers.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        btnCreateChat=(Button)findViewById(R.id.btn_create_chat);
        btnCreateChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mode == null) {

                    int countChoice = lstUsers.getCount();

                    if (lstUsers.getCheckedItemPositions().size() == 1)
                        createPrivateChat(lstUsers.getCheckedItemPositions());
                    else if (lstUsers.getCheckedItemPositions().size() > 1)
                        createGroupChat(lstUsers.getCheckedItemPositions());
                    else
                        Toast.makeText(ListUsersActivity.this, "Please select friend to chat", Toast.LENGTH_SHORT).show();
                }
                else if (mode.equals(Common.UPDATE_ADD_MODE)&&qbChatDialog!=null){
                    if (userAdd.size()>0){
                        QBDialogRequestBuilder requestBuilder=new QBDialogRequestBuilder();

                        int cntChoice=lstUsers.getCount();
                        SparseBooleanArray checkedItemPositions=lstUsers.getCheckedItemPositions();
                        for (int i=0;i<cntChoice;i++){
                            if (checkedItemPositions.get(i)){
                                QBUser user=(QBUser)lstUsers.getItemAtPosition(i);
                                requestBuilder.addUsers(user);
                            }
                        }

                        //call services
                        QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                .performAsync(new QBEntityCallback<QBChatDialog>() {
                                    @Override
                                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                        Toast.makeText(getBaseContext(), "Add User success", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {

                                    }
                                });
                    }
                }else if (mode.equals(Common.UPDATE_REMOVE_MODE)&&qbChatDialog!=null){
                    if (userAdd.size()>0){
                        QBDialogRequestBuilder requestBuilder=new QBDialogRequestBuilder();

                        int cntChoice=lstUsers.getCount();
                        SparseBooleanArray checkedItemPositions=lstUsers.getCheckedItemPositions();
                        for (int i=0;i<cntChoice;i++){
                            if (checkedItemPositions.get(i)){
                                QBUser user=(QBUser)lstUsers.getItemAtPosition(i);
                                requestBuilder.removeUsers(user);
                            }
                        }

                        //call services
                        QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                .performAsync(new QBEntityCallback<QBChatDialog>() {
                                    @Override
                                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                        Toast.makeText(getBaseContext(), "Remove User success", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {

                                    }
                                });
                    }
                }
            }
        });

        if (mode==null&&qbChatDialog==null){
            retreiveAllUser();
        }else{
            if (mode.equals(Common.UPDATE_ADD_MODE))
                loadListAvailableUser();
            else if (mode.equals(Common.UPDATE_REMOVE_MODE))
                loadListUserInGroup();
        }
    }

    private void loadListUserInGroup() {
        btnCreateChat.setText("remove User");
        QBRestChatService.getChatDialogById(qbChatDialog.getDialogId())
                .performAsync(new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                        List<Integer> occupantsId=qbChatDialog.getOccupants();
                        List<QBUser> listUserAlreadyInGroup=QBUsersHolder.getInstance().getUsersByIds(occupantsId);
                        ArrayList<QBUser> users=new ArrayList<>();
                        users.addAll(listUserAlreadyInGroup);

                        ListUsersAdapter adapter=new ListUsersAdapter(getBaseContext(),users);
                        lstUsers.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        userAdd=users;
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(ListUsersActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadListAvailableUser() {
        btnCreateChat.setText("Add User");

        QBRestChatService.getChatDialogById(qbChatDialog.getDialogId())
                .performAsync(new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                        ArrayList<QBUser> listUsers=QBUsersHolder.getInstance().getAllUsers();
                        List<Integer> occupantsId=qbChatDialog.getOccupants(); //occupantsids are the id's of members who are already in chat group
                        List<QBUser> listUserAlreadyInChatGroup=QBUsersHolder.getInstance().getUsersByIds(occupantsId);

                        //remove all users who are already in chat group
                        for (QBUser user:listUserAlreadyInChatGroup)
                            listUsers.remove(user);
                        if (listUsers.size()>0)
                        {
                            ListUsersAdapter adapter=new ListUsersAdapter(getBaseContext(),listUsers);
                            lstUsers.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                            userAdd=listUsers;
                        }
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(ListUsersActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createGroupChat(SparseBooleanArray checkedItemPositions) {

        final ProgressDialog mDialog=new ProgressDialog(ListUsersActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        int countChoice=lstUsers.getCount();
        ArrayList<Integer> occupantIdsList=new ArrayList<>();
        for(int i=0;i<countChoice;i++){
            if(checkedItemPositions.get(i)){
                QBUser user=(QBUser)lstUsers.getItemAtPosition(i);
                occupantIdsList.add(user.getId());
            }
        }

        //create chat dialog
        QBChatDialog dialog=new QBChatDialog();
        dialog.setName(Common.createChatDialogName(occupantIdsList));
        dialog.setType(QBDialogType.GROUP);
        dialog.setOccupantsIds(occupantIdsList);

        QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                mDialog.dismiss();
                Toast.makeText(getBaseContext(),"Created chat dialog successfully!",Toast.LENGTH_SHORT).show();

                //send system message to recepient id user
                QBSystemMessagesManager qbSystemMessagesManager=QBChatService.getInstance().getSystemMessagesManager();
                QBChatMessage qbChatMessage=new QBChatMessage();
                qbChatMessage.setBody(qbChatDialog.getDialogId());
                for (int i=0;i<qbChatDialog.getOccupants().size();i++){
                    qbChatMessage.setRecipientId(qbChatDialog.getOccupants().get(i));
                    try {
                        qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
                }


                finish();
            }

            @Override
            public void onError(QBResponseException e) {
              Log.e("ERROR",""+e.getMessage());
            }
        });

    }

    private void createPrivateChat(SparseBooleanArray checkedItemPositions) {

        final ProgressDialog mDialog=new ProgressDialog(ListUsersActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        int countChoice=lstUsers.getCount();
        for(int i=0;i<countChoice;i++){
            if(checkedItemPositions.get(i)){
                final QBUser user=(QBUser)lstUsers.getItemAtPosition(i);
                QBChatDialog dialog= DialogUtils.buildPrivateDialog(user.getId());

                QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                        mDialog.dismiss();
                        Toast.makeText(getBaseContext(),"Created private chat dialog successfully!",Toast.LENGTH_SHORT).show();

                        //send system message to recepient id user
                        QBSystemMessagesManager qbSystemMessagesManager=QBChatService.getInstance().getSystemMessagesManager();
                        QBChatMessage qbChatMessage=new QBChatMessage();
                        qbChatMessage.setRecipientId(user.getId());
                        qbChatMessage.setBody(qbChatDialog.getDialogId());
                        try {
                            qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }

                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR",""+e.getMessage());
                    }
                });
            }
        }

    }

    private void retreiveAllUser() {
        QBUsers.getUsers(null).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                //Add to Cache
                QBUsersHolder.getInstance().putUsers(qbUsers);

                //creates array list to add all users from web services instead of current user logged
                ArrayList<QBUser> qbUserWithoutCurrent=new ArrayList<QBUser>();
                for(QBUser user:qbUsers){
                    if(!user.getLogin().equals(QBChatService.getInstance().getUser().getLogin())){
                        qbUserWithoutCurrent.add(user);

                    }
                }
                ListUsersAdapter adapter=new ListUsersAdapter(getBaseContext(),qbUserWithoutCurrent);
                lstUsers.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR",""+e.getMessage());
            }
        });
    }
}
