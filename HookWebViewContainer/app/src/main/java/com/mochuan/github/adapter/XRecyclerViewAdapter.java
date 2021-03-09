package com.mochuan.github.adapter;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mochuan.github.R;
import com.mochuan.github.util.RouterManager;
import com.mochuan.github.util.UrlCacheUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @version mochuan.zhb on 2020-12-24
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 首页列表的Adapter
 */
public class XRecyclerViewAdapter extends RecyclerView.Adapter<XRecyclerViewAdapter.XViewHolder> {

    private List<String> data = new ArrayList<>();

    @NonNull
    @Override
    public XViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_text, parent, false);
        return new XViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull XViewHolder holder, final int position) {
        try {
            final String item = data.get(position);
            holder.mUrlTextView.setText(item);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RouterManager.openH5Activity(item);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    showPopMenu(view, position);
                    return true;
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void showPopMenu(final View view, final int pos) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                final String url = data.remove(pos);
                UrlCacheUtils.remove(url);
                notifyDataSetChanged();
                Toast.makeText(view.getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * 添加URL
     *
     * @param url
     */
    public void addUrl(String url) {
        this.data.add(url);
        notifyDataSetChanged();
    }

    /**
     * 添加URL
     *
     * @param url
     */
    public void addUrl(List<String> url) {
        this.data.addAll(url);
        notifyDataSetChanged();
    }

    public void addUrl(Set<String> url) {
        for (String item : data) {
            if (url.contains(item)) {
                url.remove(item);
            }
        }
        this.data.addAll(url);
        notifyDataSetChanged();
    }

    public void addUrl(int index, Set<String> url) {
        this.data.addAll(index, url);
        notifyDataSetChanged();
    }


    public static class XViewHolder extends RecyclerView.ViewHolder {

        public TextView mUrlTextView;

        public XViewHolder(@NonNull View itemView) {
            super(itemView);
            mUrlTextView = itemView.findViewById(R.id.title);
        }
    }

}
