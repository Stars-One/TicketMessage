package com.wan.ticketmessage.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cy.cyrvadapter.adapter.RVAdapter;
import com.cy.cyrvadapter.recyclerview.GridRecyclerView;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.wan.ticketmessage.BaseActivity;
import com.wan.ticketmessage.Bean.Message;
import com.wan.ticketmessage.R;
import com.wan.ticketmessage.Util.MessageExtract;
import com.wan.ticketmessage.Util.PermissionUtils;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;


public class MainActivity extends BaseActivity implements View.OnClickListener {

    private GridRecyclerView mRv;
    private FloatingActionButton mFloatbutton;
    private List<com.wan.ticketmessage.Bean.Message> messages;
    private RVAdapter<Message> adapter;
    final String SMS_URI_ALL = "content://sms/"; // 所有短信
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionUtils.checkAndRequestPermission(this, Manifest.permission.READ_SMS, 1);

    }

    @Override
    public void initData() {
        if (LitePal.isExist(Message.class)) {
            updateDate();
        } else {
            LitePal.getDatabase();//初始化
            messages = new ArrayList<>();
        }

        /*Message message = MessageExtract.getMessage("【智行】02月23日，玉林（11:40）-桂林，D8482抢票成功，取票号EE86285206，万兴兴 二等座05车厢08D号。");
        Message message1 = MessageExtract.getMessage("【智行】07月23日，玉林（11:40）-桂林，D8482抢票成功，取票号EE86285206，万兴兴 二等座05车厢08D号。");
        message.save();
        message1.save();

        for (int i = 0; i < 5; i++) {
            com.wan.ticketmessage.Bean.Message messagetemp = new com.wan.ticketmessage.Bean.Message("D2354" + i, "玉林" + i, "桂林", "2018/5/6 21:45", "21:52", "5车", "4D号");
            messages.add(messagetemp);
        }*/
    }

    //更新日期，判断是否过期
    private void updateDate() {
        messages = LitePal.findAll(Message.class);
        Log.d("---数据库删除是否成功---", "updateDate: " + messages.size());
        for (int i = 0; i < messages.size(); i++) {

            messages.get(i).setOutDate();
            messages.get(i).save();
        }
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void initView() {
        android.support.v7.widget.Toolbar toolbar = findView(R.id.toolbar);
        setSupportActionBar(toolbar);
        refreshLayout = findView(R.id.refreshlayout);
        refreshLayout.setColorSchemeColors(R.color.colorPrimary);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
        mRv = findView(R.id.rv);
        mFloatbutton = findView(R.id.floatbutton);
        mFloatbutton.setOnClickListener(this);


        adapter = new RVAdapter<Message>(messages) {
            @Override
            public void bindDataToView(RVViewHolder holder, int position, Message bean, boolean isSelected) {
                holder.setText(R.id.trainNumber, bean.getTrainNumber());
                holder.setText(R.id.busNumber, bean.getBusNumber());
                holder.setText(R.id.startStation, bean.getStartStation());
                holder.setText(R.id.endStation, bean.getEndStation());
                holder.setText(R.id.startTime, bean.getStartTime());
                holder.setText(R.id.arriveTime, bean.getArriveTime());
                holder.setText(R.id.busNumber, bean.getBusNumber());
                holder.setText(R.id.seatNumber, bean.getSeatNumber());
                bean.setOutDate();
                if (bean.isOutDate()) {
                    holder.setVisible(R.id.outImg);
                } else {
                    holder.setViewGone(R.id.outImg);
                }

            }

            @Override
            public int getItemLayoutID(int position, Message bean) {
                return R.layout.item_rv;
            }

            @Override
            public void onItemClick(int position, Message bean) {

            }

            @Override
            public void onItemLongClick(int position, final Message bean) {
                View v = mRv.getChildAt(position);
                final int myposition = position;//注意这个position
                XPopup.get(v.getContext()).asAttachList(new String[]{"删除"},
                        null,
                        new OnSelectListener() {
                            @Override
                            public void onSelect(int position, String text) {
                                messages.remove(bean);
                                adapter.notifyItemRemoved(myposition);//移出item
                                showSnackBar(bean, myposition);//展示SnackBar,允许撤销操作
                            }
                        })
                        .atView(v.findViewById(R.id.seatNumber))  // 如果是要依附某个View，必须设置
                        .hasShadowBg(false)
                        .show();

            }
        };
        mRv.setItemAnimator(new SlideInLeftAnimator());
        mRv.getItemAnimator().setRemoveDuration(600);
        mRv.setAdapter(MainActivity.this, adapter, 1, RecyclerView.VERTICAL);
    }

    /**
     * 下拉刷新数据
     */
    private void refreshData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int oldSize = LitePal.findAll(Message.class).size();
                List<String> list = getSMSBody();
                if (list.size() == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "未找到可添加的短信", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    extraData(list);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int newSize = LitePal.findAll(Message.class).size();
                if (newSize > oldSize) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "已添加", Toast.LENGTH_SHORT).show();
                            refreshLayout.setRefreshing(false);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "未找到可添加的短信", Toast.LENGTH_SHORT).show();
                            refreshLayout.setRefreshing(false);
                        }
                    });
                }


            }
        }).start();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_delete:
                getDeleteALLData();
                break;
            case R.id.item_about:
                startActivity(Main2Activity.class);
            default:
                break;
        }
        return true;
    }

    private void showSnackBar(final Message message, final int position) {
        Snackbar.make(refreshLayout, "已成功删除数据", Snackbar.LENGTH_SHORT).addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_CONSECUTIVE) {
                    //通过对象的delete删除，litepal使用指定id删除的话，会出现找不到id，可能是因为Bean中未定义id属性
                    message.delete();
                }
            }
        }).setAction("撤销删除", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messages.add(position, message);
                adapter.notifyItemInserted(position);
            }
        }).show();
    }


    /**
     * 获得全部过期车票信息
     */
    private void getDeleteALLData() {

        while (true) {
            if (findData()) {
                deleteData();
            } else {
                break;
            }
        }
        showToast("已删除过期车票！");
    }

    private boolean findData() {
        for (Message message : messages) {
            if (message.isOutDate()) {
                return true;
            }
        }
        return false;
    }


    /**
     * 删除一条过期数据
     */
    private void deleteData() {
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message.isOutDate()) {
                message.delete();//数据库删除
                adapter.notifyItemRemoved(i);
                messages.remove(message);
                break;
            }
        }
    }

    /**
     * 找出该message在List中的位置，实现删除效果
     *
     * @param message
     * @return
     */
    private int findPosition(Message message) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).equals(message)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatbutton:
                final View view = getLayoutInflater().inflate(R.layout.layout_dialog, null);
                new AlertDialog.Builder(this).setView(view).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setPositiveButton("添加", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextInputEditText textInputEditText = (TextInputEditText) view.findViewById(R.id.ed);
                        getInputData(textInputEditText);
                    }
                }).setTitle("输入内容").show();

                break;
            default:
                break;
        }
    }

    /**
     * 添加数据
     *
     * @param textInputEditText String信息
     */
    private void getInputData(TextInputEditText textInputEditText) {
        String data = textInputEditText.getText().toString();
        if (!TextUtils.isEmpty(data)) {
            if (data.startsWith("【智行】")) {
                Message message = MessageExtract.getMessage(data);
                message.save();
                adapter.add(message);//适配器绑定的数据源添加一条数据
                showToast("添加成功！");
            } else {
                showToast("内容错误!");
            }
        } else {
            showToast("内容不能为空!!");
        }

    }

    /**
     * 提取全部短信内容
     *
     * @param list
     */
    private void extraData(List<String> list) {


        for (int i = 0; i < list.size(); i++) {
            String data = list.get(i);
            if (!TextUtils.isEmpty(data)) {
                if (data.startsWith("【智行】")) {
                    final Message message = MessageExtract.getMessage(data);
                    //不相同才存进数据库
                    if (!isSame(message)) {
                        Log.d("---提取短信---", "extraData: 存入数据");
                        message.save();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyItemInserted(adapter.getItemCount());//末尾插入
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * 判断是否相同
     *
     * @param message 信息
     * @return 是否相同
     */
    private boolean isSame(Message message) {
        boolean flag = false;
        List<Message> list = LitePal.findAll(Message.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(message)) {
                flag = true;//存在一个相同，即可返回true,停止循环
                break;
            }
        }
        return flag;
    }


    /**
     * 获取全部短信内容
     *
     * @return
     */
    private List<String> getSMSBody() {
        List<String> bodyList = new ArrayList<>();
        if (PermissionUtils.checkPermission(this, Manifest.permission.READ_SMS)) {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[]{"_id", "address", "person",
                    "body", "date", "type",};
            Cursor cur = getContentResolver().query(uri, projection, null,
                    null, "date desc");
            int index_Body = cur.getColumnIndex("body");
            //Cursor下标是从-1开始的，所以要移动到first（下标为0）
            if (cur.moveToFirst()) {
                do {
                    String strbody = cur.getString(index_Body);
                    bodyList.add(strbody);
                } while (cur.moveToNext());
            }

            return bodyList;
        } else {
            showToast("请允许短信权限");
            return null;
        }

    }


}
