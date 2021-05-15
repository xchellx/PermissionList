package xjonx.permlist;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.l4digital.fastscroll.FastScroller;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@SuppressWarnings("unused")
public class PermissionListAdapter extends RecyclerView.Adapter<PermissionListAdapter.ViewHolder> implements FastScroller.SectionIndexer {
    // Adapter data filter options
    private final @NonNull EnumSet<FilterOptions> listItemsFilterOptions = EnumSet.of(FilterOptions.PERMISSION);
    // Adapter data sorting method
    private final @NonNull Comparator<ListItem> listItemsComparator = (a, b) ->
            a.getSectionText(false).compareToIgnoreCase(b.getSectionText(false));
    // Adapter data
    private final @NonNull SortedList<ListItem> listItems = new SortedList<>(ListItem.class, new SortedList.Callback<ListItem>() {
        @Override  public void onInserted(int position, int count) { notifyItemRangeInserted(position, count); }
        @Override public void onRemoved(int position, int count) { notifyItemRangeRemoved(position, count); }
        @Override public void onMoved(int fromPosition, int toPosition) { notifyItemMoved(fromPosition, toPosition); }
        @Override public void onChanged(int position, int count) { notifyItemRangeChanged(position, count); }
        @Override public int compare(ListItem a, ListItem b) { return listItemsComparator.compare(a, b); }
        @Override public boolean areContentsTheSame(ListItem oldItem, ListItem newItem) { return oldItem.equals(newItem); }
        @Override public boolean areItemsTheSame(ListItem a, ListItem b) { return a.equals(b); }
    });
    // Adapter data filter options updated callback
    private @Nullable OnFilterOptionsUpdatedListener onFilterOptionsUpdatedListener;
    // Adapter item click callback
    private @Nullable OnListItemClickListener onListItemClickListener;

    public PermissionListAdapter() { this(null); }
    public PermissionListAdapter(List<ListItem> pListItems) { this(pListItems, null); }
    public PermissionListAdapter(List<ListItem> pListItems, EnumSet<FilterOptions> pListItemsFilterOptions) {
        this(pListItems, pListItemsFilterOptions, null);
    }
    public PermissionListAdapter(List<ListItem> pListItems, EnumSet<FilterOptions> pListItemsFilterOptions,
                                 @Nullable OnFilterOptionsUpdatedListener pOnFilterOptionsUpdatedListener) {
        this(pListItems, pListItemsFilterOptions, pOnFilterOptionsUpdatedListener, null);
    }
    public PermissionListAdapter(List<ListItem> pListItems, EnumSet<FilterOptions> pListItemsFilterOptions,
                                 @Nullable OnFilterOptionsUpdatedListener pOnFilterOptionsUpdatedListener,
                                 @Nullable OnListItemClickListener pOnListItemClickListener) {
        addAll(pListItems);
        setFilterOptions(pListItemsFilterOptions);
        setOnFilterOptionsUpdatedListener(pOnFilterOptionsUpdatedListener);
        setOnListItemClickListener(pOnListItemClickListener);
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View permlist_item = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.permlist_item, viewGroup, false);
        return new ViewHolder(permlist_item);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if (onListItemClickListener != null) {
            viewHolder.getPermListItem().setOnClickListener((View view) -> onListItemClickListener.onListItemClick(get(position)));
        }
        viewHolder.getPermListItemPermName().setText(listItems.get(position).getPermissionName());
        if (listItems.get(position).getIsRevocable()) {
            viewHolder.getPermListItemIsRevocable().setVisibility(View.VISIBLE);
        } else {
            viewHolder.getPermListItemIsRevocable().setVisibility(View.GONE);
        }
        viewHolder.getPermListItemPackageName().setText(listItems.get(position).getPackageName());
        if (listItems.get(position).icon != null) {
            viewHolder.getPermListItemIcon().setImageDrawable(listItems.get(position).icon);
        }
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    @Override
    public CharSequence getSectionText(int position) {
        return listItems.get(position).getSectionText(true).toUpperCase();
    }

    public void addFilterOption(FilterOptions pListItemsFilterOption) {
        if (pListItemsFilterOption != null) {
            listItemsFilterOptions.add(pListItemsFilterOption);
            if (onFilterOptionsUpdatedListener != null) {
                onFilterOptionsUpdatedListener.onFilterOptionsUpdated();
            }
        }
    }

    public void removeFilterOption(FilterOptions pListItemsFilterOption) {
        if (pListItemsFilterOption != null) {
            listItemsFilterOptions.remove(pListItemsFilterOption);
            if (onFilterOptionsUpdatedListener != null) {
                onFilterOptionsUpdatedListener.onFilterOptionsUpdated();
            }
        }
    }

    public void setFilterOption(FilterOptions pListItemsFilterOption, boolean set) {
        if (pListItemsFilterOption != null) {
            if (set) {
                addFilterOption(pListItemsFilterOption);
            } else {
                removeFilterOption(pListItemsFilterOption);
            }
        }
    }

    private void setFilterOptions(EnumSet<FilterOptions> pListItemsFilterOptions) {
        if (pListItemsFilterOptions != null) {
            listItemsFilterOptions.clear();
            listItemsFilterOptions.addAll(pListItemsFilterOptions);
            if (onFilterOptionsUpdatedListener != null) {
                onFilterOptionsUpdatedListener.onFilterOptionsUpdated();
            }
        }
    }

    public boolean getFilterOption(FilterOptions pListItemsFilterOption) {
        if (pListItemsFilterOption != null) {
            return listItemsFilterOptions.contains(pListItemsFilterOption);
        } else {
            return false;
        }
    }

    public @NonNull EnumSet<FilterOptions> getFilterOptions() { return listItemsFilterOptions; }

    public void setOnFilterOptionsUpdatedListener(@Nullable OnFilterOptionsUpdatedListener pOnFilterOptionsUpdatedListener) {
        onFilterOptionsUpdatedListener = pOnFilterOptionsUpdatedListener;
    }

    public void setOnListItemClickListener(@Nullable OnListItemClickListener pOnListItemClickListener) {
        onListItemClickListener = pOnListItemClickListener;
    }

    public void filter(List<ListItem> items) throws InvalidFilterOptionsException { filter(items, ""); }

    public void filter(List<ListItem> items, @Nullable String query) throws InvalidFilterOptionsException {
        if (items != null) {
            final String lQuery = (query == null ? "" : query).trim().toLowerCase();
            final List<ListItem> filteredItems = new ArrayList<>();
            for (ListItem item : items) {
                boolean filterPass = false;
                if (listItemsFilterOptions.contains(FilterOptions.PERMISSION) && listItemsFilterOptions.contains(FilterOptions.PACKAGE)) {
                    throw new InvalidFilterOptionsException("Invalid filter configuration: " + FilterOptions.PERMISSION.toString() +
                            " and " + FilterOptions.PACKAGE.toString() + " cannot be set at the same time.");
                } else if (listItemsFilterOptions.contains(FilterOptions.PERMISSION) && !listItemsFilterOptions.contains(FilterOptions.PACKAGE)) {
                    filterPass = item.getPermissionName().toLowerCase().contains(lQuery.equals("") ? item.getPermissionName().toLowerCase() : lQuery);
                } else if (!listItemsFilterOptions.contains(FilterOptions.PERMISSION) && listItemsFilterOptions.contains(FilterOptions.PACKAGE)) {
                    filterPass = item.getPackageName().toLowerCase().contains(lQuery.equals("") ? item.getPackageName().toLowerCase() : lQuery);
                }
                if (listItemsFilterOptions.contains(FilterOptions.REVOCABLE)) {
                    filterPass = filterPass && item.getIsRevocable();
                }
                if (filterPass) {
                    filteredItems.add(item);
                }
            }
            replaceAll(filteredItems);
        }
    }

    public void clear() { listItems.clear(); }

    public ListItem get(int position) {
        return listItems.get(position);
    }

    public void add(ListItem item) {
        if (item != null) {
            listItems.add(item);
        }
    }

    public void remove(ListItem item) {
        if (item != null) {
            listItems.remove(item);
        }
    }

    public void addAll(List<ListItem> items) {
        if (items != null) {
            listItems.addAll(items);
        }
    }

    public void removeAll(List<ListItem> items) {
        if (items != null) {
            listItems.beginBatchedUpdates();
            for (ListItem item : items) {
                listItems.remove(item);
            }
            listItems.endBatchedUpdates();
        }
    }

    public void replaceAll(List<ListItem> items) {
        if (items != null) {
            listItems.beginBatchedUpdates();
            for (int i = listItems.size() - 1; i >= 0; i--) {
                final ListItem item = listItems.get(i);
                if (!items.contains(item)) {
                    listItems.remove(item);
                }
            }
            listItems.addAll(items);
            listItems.endBatchedUpdates();
        }
    }

    public enum FilterOptions {
        PERMISSION, PACKAGE, REVOCABLE
    }

    public interface OnFilterOptionsUpdatedListener {
        public void onFilterOptionsUpdated();
    }

    public interface OnListItemClickListener {
        public void onListItemClick(ListItem listItem);
    }

    public static class InvalidFilterOptionsException extends RuntimeException {
        public InvalidFilterOptionsException() { super(); }
        public InvalidFilterOptionsException(String s) { super(s); }
        public InvalidFilterOptionsException(String message, Throwable cause) { super(message, cause); }
        public InvalidFilterOptionsException(Throwable cause) { super(cause); }
        private static final long serialVersionUID = 6592389128676302495L;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final @NonNull CardView permlist_item;
        private final @NonNull TextView permlist_item_permname;
        private final @NonNull ImageView permlist_item_packageicon;
        private final @NonNull TextView permlist_item_packagename;
        private final @NonNull TextView permlist_item_isrevocable;

        public ViewHolder(View view) {
            super(view);
            permlist_item = view.findViewById(R.id.permlist_item);
            permlist_item_permname = view.findViewById(R.id.permlist_item_permname);
            permlist_item_packageicon = view.findViewById(R.id.permlist_item_packageicon);
            permlist_item_packagename = view.findViewById(R.id.permlist_item_packagename);
            permlist_item_isrevocable = view.findViewById(R.id.permlist_item_isrevocable);
        }

        public @NonNull CardView getPermListItem() { return permlist_item; }
        public @NonNull TextView getPermListItemPermName() {
            return permlist_item_permname;
        }
        public @NonNull ImageView getPermListItemIcon() { return permlist_item_packageicon; }
        public @NonNull TextView getPermListItemPackageName() {
            return permlist_item_packagename;
        }
        public @NonNull TextView getPermListItemIsRevocable() {
            return permlist_item_isrevocable;
        }
    }

    public static class ListItem {
        private @Expose @NonNull String permName = "";
        private @Nullable Drawable icon = null;
        private @Expose @NonNull String packageName = "";
        private @Expose boolean isRevocable = false;
        private int hash = 0;

        public ListItem() {}
        public ListItem(@Nullable String pName) { this(pName, false); }
        public ListItem(@Nullable String pName, boolean pIsRevocable) { this(pName, pIsRevocable, null); }
        public ListItem(@Nullable String pName, boolean pIsRevocable, @Nullable String pPackageName) { this(pName, pIsRevocable, pPackageName, null); }
        public ListItem(@Nullable String pName, boolean pIsRevocable, @Nullable String pPackageName, @Nullable Drawable pIcon) {
            setPermissionName(pName);
            setIsRevocable(pIsRevocable);
            setPackageName(pPackageName);
            setIcon(pIcon);
        }

        public @NonNull String getPermissionName() { return permName; }
        public @Nullable Drawable getIcon() { return icon; }
        public @NonNull String getPackageName() { return packageName; }
        public boolean getIsRevocable() { return isRevocable; }

        public void setPermissionName(@Nullable String pName) { permName = (pName == null) ? "" : pName.trim(); }
        public void setIcon(@Nullable Context ctx, int pIcon) { setIcon((ctx == null) ? null : ContextCompat.getDrawable(ctx, pIcon)); }
        public void setIcon(@Nullable Drawable pIcon) { icon = pIcon; }
        public void setPackageName(@Nullable String pPackageName) { packageName = (pPackageName == null) ? "" : pPackageName.trim(); }
        public void setIsRevocable(boolean pIsRevocable) { isRevocable = pIsRevocable; }

        public @NonNull String getSectionText(boolean letterOnly) {
            int lastDotStart = permName.lastIndexOf('.');
            lastDotStart = (lastDotStart == -1) ? 0 : lastDotStart + 1;
            int lastDotEnd = (lastDotStart == 0) ? 1 : lastDotStart + 1;
            return permName.substring(lastDotStart, letterOnly ? lastDotEnd : permName.length());
        }

        @Override
        public int hashCode() {
            int h = hash;
            String hStr = permName + isRevocable + packageName;
            final int len = hStr.length();
            if (h == 0) {
                if (len > 0) {
                    for (int i = 0; i < len; i++) {
                        h = 31 * h + hStr.charAt(i);
                    }
                }
                h = 31 * h + (isRevocable ? 0 : 1);
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                // Compared object has the same reference
                return true;
            } else if (obj == null) {
                // Compared object is null
                return false;
            } else if (getClass() != obj.getClass()) {
                // Compared object is not the same class
                return false;
            } else {
                // Compare object fields
                final ListItem other = (ListItem) obj;

                // #name
                if (permName == null) {
                    if (other.permName != null)
                        return false;
                } else if (!permName.equals(other.permName)) {
                    return false;
                }

                // #packageName
                if (packageName == null) {
                    if (other.packageName != null) {
                        return false;
                    }
                } else if (!packageName.equals(other.packageName)) {
                    return false;
                }

                // #isRevocable
                if (isRevocable != other.isRevocable) {
                    return false;
                }

                // All is equal
                return true;
            }
        }

        @Override
        public @NonNull String toString() {
            return getClass().getName() + "@" + Integer.toHexString(hashCode());
        }

        public static class Serializer implements JsonSerializer<ListItem> {
            @Override
            public JsonElement serialize(ListItem src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject listItem = new JsonObject();
                listItem.addProperty("permission_name", src.getPermissionName());
                listItem.addProperty("package_name", src.getPackageName());
                listItem.addProperty("is_revocable", src.getIsRevocable());
                return listItem;
            }
        }
    }
}
