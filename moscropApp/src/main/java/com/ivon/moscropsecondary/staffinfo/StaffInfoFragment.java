package com.ivon.moscropsecondary.staffinfo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.ivon.moscropsecondary.MainActivity;
import com.ivon.moscropsecondary.R;

import java.util.ArrayList;
import java.util.List;

public class StaffInfoFragment extends Fragment implements AdapterView.OnItemClickListener {

    private int mPosition;
    private ListView mListView;

    private StaffListAdapter mAdapter;

    public static StaffInfoFragment newInstance(int position) {
    	StaffInfoFragment fragment = new StaffInfoFragment();
        fragment.mPosition = position;
    	return fragment;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	setHasOptionsMenu(true);
    	
    	View rootView = inflater.inflate(R.layout.fragment_teachers, container, false);
        mListView = (ListView) rootView.findViewById(R.id.teachers_list);
        mAdapter = new StaffListAdapter(getActivity(), new ArrayList<StaffInfoModel>());
    	mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                refreshList(null);
            }
        }).start();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity) getActivity()).onSectionAttached(mPosition);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem search = menu.findItem(R.id.action_search);
        if(search != null) {
            search.setVisible(true);
            SearchView searchView = (SearchView) search.getActionView();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Toast.makeText(getActivity(), "Searching for " + query, Toast.LENGTH_SHORT).show();
                    refreshList(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText == null || newText.equals("")) {
                        refreshList(null);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.staff_info_dialog, null);

        StaffInfoModel model = mAdapter.getItem(position);
        String name = model.getName();
        String[] subjects = model.getSubjects();
        int[] rooms = model.getRooms();
        String email = model.getEmail();
        String website = model.getSite();

        if (subjects != null && subjects.length > 0) {
            View subjectGroup = dialogView.findViewById(R.id.subject_group);
            subjectGroup.setVisibility(View.VISIBLE);
            TextView subjectTitle = (TextView) dialogView.findViewById(R.id.subject_title);
            if (subjects.length > 1) {
                subjectTitle.append("s");
            }
            TextView subjectText = (TextView) dialogView.findViewById(R.id.subject);
            for (int i=0; i<subjects.length; i++) {
                if (i != 0) {
                    // Not the first item
                    subjectText.append(", " + subjects[i]);
                } else {
                    subjectText.setText(subjects[i]);
                }
            }
        } else {
            View subjectGroup = dialogView.findViewById(R.id.subject_group);
            subjectGroup.setVisibility(View.GONE);
        }

        if (rooms != null && rooms.length > 0) {
            View roomGroup = dialogView.findViewById(R.id.room_group);
            roomGroup.setVisibility(View.VISIBLE);
            TextView roomTitle = (TextView) dialogView.findViewById(R.id.room_title);
            if (rooms.length > 1) {
                roomTitle.append("s");
            }
            TextView roomText = (TextView) dialogView.findViewById(R.id.room);
            for (int i=0; i<rooms.length; i++) {
                if (i != 0) {
                    // Not the first item
                    roomText.append(", " + rooms[i]);
                } else {
                    roomText.setText(String.valueOf(rooms[i]));
                }
            }
        } else {
            View roomGroup = dialogView.findViewById(R.id.room_group);
            roomGroup.setVisibility(View.GONE);
        }

        if (email != null && !email.equals("")) {
            View emailGroup = dialogView.findViewById(R.id.email_group);
            emailGroup.setVisibility(View.VISIBLE);
            TextView emailText = (TextView) dialogView.findViewById(R.id.email);
            emailText.setText(email);
        } else {
            View emailGroup = dialogView.findViewById(R.id.email_group);
            emailGroup.setVisibility(View.GONE);
        }

        if (website != null && !website.equals("")) {
            View websiteGroup = dialogView.findViewById(R.id.website_group);
            websiteGroup.setVisibility(View.VISIBLE);
            TextView websiteText = (TextView) dialogView.findViewById(R.id.website);
            websiteText.setText(website);
        } else {
            View websiteGroup = dialogView.findViewById(R.id.website_group);
            websiteGroup.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(name);
        builder.setView(dialogView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


    private void refreshList(String query) {

        // This line does all the copying (if needed)
        StaffInfoDatabase db = new StaffInfoDatabase(getActivity());

        // This line is the normal database stuff
        final List<StaffInfoModel> models = db.getList(query);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.clear();
                mAdapter.addAll(models);
                mAdapter.notifyDataSetChanged();
            }
        });
    }
}