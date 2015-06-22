package biz.no_ip.danie_dutoit.clashofclans;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.HashMap;

/**
 * Created by Danie on 2015/06/06.
 */
public class SelectResultCustomGrid extends BaseAdapter {
    private Context mContext;
    private String[] web;
    private int[] Imageid;

    public SelectResultCustomGrid(Context c, String[] web, int[] Imageid) {
        mContext = c;
        this.Imageid = Imageid;
        this.web = web;
    }

    @Override
    public int getCount() {
        return web.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View grid;
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            grid = inflater.inflate(R.layout.select_result_grid_single, null);
            ImageView imageView = (ImageView) grid.findViewById(R.id.select_result_grid_image);
            imageView.setImageResource(Imageid[position]);

        } else {
            grid = (View) convertView;
        }

        return grid;
    }
}
