package e.pavanmalisetti.androidchatapp.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.quickblox.chat.model.QBChatDialog;

import java.util.ArrayList;

import e.pavanmalisetti.androidchatapp.Holder.QBUnreadMessagesHolder;
import e.pavanmalisetti.androidchatapp.R;

public class ChatDialogsAdapters extends BaseAdapter {

    private Context context;
    private ArrayList<QBChatDialog> qbChatDialogs;

    public ChatDialogsAdapters(Context context,ArrayList<QBChatDialog> qbChatDialogs){
        this.context=context;
        this.qbChatDialogs=qbChatDialogs;
    }

    @Override
    public int getCount() {
        return qbChatDialogs.size();
    }

    @Override
    public Object getItem(int position) {
        return qbChatDialogs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view=convertView;
        if(view==null){
            LayoutInflater inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view=inflater.inflate(R.layout.list_chat_dialog,null);

            TextView txtTitle,txtMessage;
            ImageView imageView,image_unread;

            txtMessage=(TextView)view.findViewById(R.id.list_chat_dialog_message);
            txtTitle=(TextView)view.findViewById(R.id.list_chat_dialog_title);
            imageView=(ImageView)view.findViewById(R.id.image_chatDialog);
            image_unread=(ImageView)view.findViewById(R.id.image_unread);

            txtMessage.setText(qbChatDialogs.get(position).getLastMessage());
            txtTitle.setText(qbChatDialogs.get(position).getName());

            ColorGenerator generator=ColorGenerator.MATERIAL;
            int randomColor=generator.getRandomColor();

            TextDrawable.IBuilder builder=TextDrawable.builder().beginConfig()
                    .withBorder(4)
                    .endConfig()
                    .round();

            //get first character from chat Dialog title for create chat dialog image
            TextDrawable drawable=builder.build(txtTitle.getText().toString().substring(0,1).toUpperCase(),randomColor);

            imageView.setImageDrawable(drawable);

            //set message unread count
            TextDrawable.IBuilder unReadBuilder=TextDrawable.builder().beginConfig()
                    .withBorder(4)
                    .endConfig()
                    .round();
            int unread_count= QBUnreadMessagesHolder.getInstance().getBundle().getInt(qbChatDialogs.get(position).getDialogId());
            if (unread_count>0){
                TextDrawable unread_drawable=unReadBuilder.build(""+unread_count,Color.RED);
                image_unread.setImageDrawable(unread_drawable);
            }

        }
        return view;
    }
}
