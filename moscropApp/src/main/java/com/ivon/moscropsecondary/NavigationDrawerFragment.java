package com.ivon.moscropsecondary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

/**
 * Created by ivon on 19/10/14.
 */
public class NavigationDrawerFragment extends NavigationDrawerBase {

    // Section Constants
    public static final int NEWS = 0;
    public static final int EMAIL = 1;
    public static final int STUDENT = 2;
    public static final int EVENTS = 3;
    public static final int TEACHERS = 4;
    public static final int SETTINGS = 5;
    public static final int ABOUT = 6;

    private NavDrawerAdapter mDrawerAdapter;
    private ListView mDrawerList;

    @Override
    public View onCreateDrawer(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mDrawerList = (ListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
        initList(inflater);
        return mDrawerList;
    }

    @Override
    protected ListView getNavigationItemsList() {
        return mDrawerList;
    }

    private void initList(LayoutInflater inflater) {

        // Initialize the adapter
        mDrawerAdapter = new NavDrawerAdapter(getActivity());   // TODO: use getSupportActionBar().getThemedContext()
        String[] drawerItems = getActivity().getResources().getStringArray(R.array.navigation_items);
        mDrawerAdapter.addItems(drawerItems);

        // Add header
        View headerView = inflater.inflate(R.layout.drawer_header, mDrawerList, false);
        mDrawerList.addHeaderView(headerView, null, false);

        // Add footer
        View footerView = inflater.inflate(R.layout.drawer_footer, mDrawerList, false);
        mDrawerList.addFooterView(footerView, null, false);

        // Set list adapter
        mDrawerList.setAdapter(mDrawerAdapter);

        // Apply onClick listeners to ListView items, footer, and header
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long index) {
                selectItem(position - mDrawerList.getHeaderViewsCount());
            }
        });
        ((Button) footerView.findViewById(R.id.btnSettings)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectItem(SETTINGS);
            }
        });
        ((Button) footerView.findViewById(R.id.btnAbout)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectItem(ABOUT);
            }
        });

        // Load banner using Picasso to initialize its singleton ahead of time
        //Picasso.with(this).load(R.drawable.banner).into((ImageView) headerView.findViewById(R.id.imgBanner));

        mDrawerList.setItemChecked(getCurrentSelectedPosition() /*+ mDrawerList.getHeaderViewsCount()*/, true);
    }
}
