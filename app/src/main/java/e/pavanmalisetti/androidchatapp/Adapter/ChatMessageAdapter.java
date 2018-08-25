package e.pavanmalisetti.androidchatapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.library.bubbleview.BubbleTextView;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBChatMessage;

import java.util.ArrayList;

import e.pavanmalisetti.androidchatapp.Holder.QBUsersHolder;
import e.pavanmalisetti.androidchatapp.R;

public class ChatMessageAdapter extends BaseAdapter{

   private Context context;
   public ArrayList<QBChatMessage> qbChatMessages;

   public ChatMessageAdapter(Context context,ArrayList<QBChatMessage> qbChatMessages){
       this.context=context;
       this.qbChatMessages=qbChatMessages;
   }

    @Override
    public int getCount() {
           return qbChatMessages.size();
    }

    @Override
    public Object getItem(int position) {
        return qbChatMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view=convertView;
        if (convertView==null){
            LayoutInflater inflater=(LayoutInflater)context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            //check if the message is send from current user
            if (qbChatMessages.get(position).getSenderId().equals(QBChatService.getInstance().getUser().getId())){
                view=inflater.inflate(R.layout.list_send_message,null);
                BubbleTextView bubbleTextView=(BubbleTextView)view.findViewById(R.id.message_content);
                bubbleTextView.setText(qbChatMessages.get(position).getBody());
            }else{
                view=inflater.inflate(R.layout.list_recv_message,null);
                BubbleTextView bubbleTextView=(BubbleTextView)view.findViewById(R.id.message_content);
                bubbleTextView.setText(qbChatMessages.get(position).getBody());
                TextView txtName=(TextView)view.findViewById(R.id.message_user);
                txtName.setText(QBUsersHolder.getInstance().getUserById(qbChatMessages.get(position).getSenderId()).getFullName());

            }
        }
        return view;
    }
}
