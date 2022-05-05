package de.baumann.browser.view;

import static de.baumann.browser.database.RecordAction.BOOKMARK_ITEM;
import static de.baumann.browser.database.RecordAction.HISTORY_ITEM;
import static de.baumann.browser.database.RecordAction.STARTSITE_ITEM;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.baumann.browser.R;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;


public class AdapterSearch extends BaseAdapter implements Filterable {
    private final Context context;
    private final int layoutResId;
    private final List<CompleteItem> originalList;
    private final CompleteFilter filter = new CompleteFilter();
    private List<CompleteItem> resultList;
    private int count;

    public AdapterSearch(Context context, int layoutResId, List<Record> recordList) {
        this.context = context;
        this.layoutResId = layoutResId;
        this.originalList = new ArrayList<>();
        this.resultList = new ArrayList<>();
        getRecordList(recordList);
    }

    private void getRecordList(List<Record> recordList) {
        for (Record record : recordList) {
            if (record.getTitle() != null
                    && !record.getTitle().isEmpty()
                    && record.getURL() != null
                    && !record.getURL().isEmpty()) {
                originalList.add(new CompleteItem(record.getTitle(), record.getURL(), record.getType()));
            }
        }

        Set<CompleteItem> set = new HashSet<>(originalList);
        originalList.clear();
        originalList.addAll(set);
    }

    @Override
    public int getCount() {
        if (count > 0) {
            return resultList.size();
        } else {
            return 0;
        }
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public Object getItem(int position) {
        return resultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Holder holder;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, null, false);
            holder = new Holder();
            holder.titleView = view.findViewById(R.id.titleView);
            holder.urlView = view.findViewById(R.id.dateView);
            holder.iconView = view.findViewById(R.id.iconView);
            holder.favicon = view.findViewById(R.id.faviconView);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        CompleteItem item = resultList.get(position);
        holder.titleView.setText(item.title);
        holder.titleView.setPadding(0,0,100,0);
        holder.urlView.setText(item.url);
        holder.iconView.setVisibility(View.VISIBLE);

        if (item.getType() == STARTSITE_ITEM) {  //Item from start page
            holder.iconView.setImageResource(R.drawable.icon_web);
        } else if (item.getType() == HISTORY_ITEM) {  //Item from history
            holder.iconView.setImageResource(R.drawable.icon_history);
        } else if (item.getType() == BOOKMARK_ITEM)
            holder.iconView.setImageResource(R.drawable.icon_bookmark);  //Item from bookmarks

        FaviconHelper faviconHelper = new FaviconHelper(context);
        Bitmap bitmap = faviconHelper.getFavicon(item.url);

        if (bitmap != null) {
            holder.favicon.setImageBitmap(bitmap);
        } else {
            holder.favicon.setImageResource(R.drawable.icon_image_broken);
        }

        return view;
    }

    private static class CompleteItem {
        private final String title;
        private final int type;
        private final String url;
        private int index = Integer.MAX_VALUE;

        private CompleteItem(String title, String url, int type) {
            this.title = title;
            this.url = url;
            this.type = type;
        }

        private int getType() {
            return this.type;
        }

        String getTitle() {
            return title;
        }

        String getURL() {
            return url;
        }

        int getIndex() {
            return index;
        }

        void setIndex(int index) {
            this.index = index;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CompleteItem)) {
                return false;
            }

            CompleteItem item = (CompleteItem) object;
            return item.getTitle().equals(title) && item.getURL().equals(url);
        }

        @Override
        public int hashCode() {
            if (title == null || url == null) {
                return 0;
            }

            return title.hashCode() & url.hashCode();
        }
    }

    private static class Holder {
        private ImageView iconView;
        private ImageView favicon;
        private TextView titleView;
        private TextView urlView;
    }

    private class CompleteFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            if (prefix == null) {
                return new FilterResults();
            }

            List<CompleteItem> workList = new ArrayList<>();
            for (CompleteItem item : originalList) {
                if (item.getTitle().contains(prefix) || item.getTitle().toLowerCase().contains(prefix) || item.getURL().contains(prefix)) {
                    if (item.getTitle().contains(prefix) || item.getTitle().toLowerCase().contains(prefix)) {
                        item.setIndex(item.getTitle().indexOf(prefix.toString()));
                    } else if (item.getURL().contains(prefix)) {
                        item.setIndex(item.getURL().indexOf(prefix.toString()));
                    }
                    workList.add(item);
                }
            }

            workList.sort(Comparator.comparingInt(CompleteItem::getIndex));

            FilterResults results = new FilterResults();
            results.values = workList;
            results.count = workList.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            count = results.count;
            if (results.count > 0) {
                // The API returned at least one result, update the data.
                resultList = (List<CompleteItem>) results.values;
                notifyDataSetChanged();
            } else {
                // The API did not return any results, invalidate the data set.
                notifyDataSetInvalidated();
            }
        }
    }
}