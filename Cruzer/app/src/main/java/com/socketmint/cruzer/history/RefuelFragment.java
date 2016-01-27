package com.socketmint.cruzer.history;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.socketmint.cruzer.CruzerApp;
import com.socketmint.cruzer.R;
import com.socketmint.cruzer.crud.CrudChoices;
import com.socketmint.cruzer.crud.Retrieve;
import com.socketmint.cruzer.database.DatabaseHelper;
import com.socketmint.cruzer.database.DatabaseSchema;
import com.socketmint.cruzer.dataholder.Refuel;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.manage.LocData;
import com.socketmint.cruzer.ui.UserInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RefuelFragment extends Fragment {
    private static final String TAG = "ViewRefuels";

    private UserInterface userInterface = UserInterface.getInstance();

    private static DatabaseHelper databaseHelper;
    private static Adapter adapter;
    private static String vId;
    private static List<Refuel> refuels;

    private Tracker analyticsTracker;

    public static RefuelFragment newInstance(String vId) {
        RefuelFragment fragment = new RefuelFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.VEHICLE_ID, vId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_refuel, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();

        userInterface.changeActivity(getActivity());
        databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());

        vId = getArguments().getString(Constants.Bundle.VEHICLE_ID);
        vId = (vId == null) ? "0" : vId;
        vId = (vId.isEmpty()) ? "0" : vId;

        initializeViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        analyticsTracker.setScreenName(TAG);
        analyticsTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private void initializeViews(View v) {
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler_history_refuel);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new Adapter(vId);
        recyclerView.setAdapter(adapter);
    }

    public static void addData() {
        try {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    refuels = (vId.equals("all")) ? databaseHelper.refuels() : databaseHelper.refuels(Arrays.asList(DatabaseSchema.COLUMN_VEHICLE_ID), new String[]{vId});
                    Collections.sort(refuels, new Comparator<Refuel>() {
                        @Override
                        public int compare(Refuel lhs, Refuel rhs) {
                            return rhs.date.compareTo(lhs.date);
                        }
                    });
                    return null;
                }
                @Override
                protected void onPostExecute(Void result) {
                    adapter.animateTo(refuels);
                }
            }.execute();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    addData();
                }
            }, 40);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public class Adapter extends RecyclerView.Adapter<Holder> implements View.OnClickListener {

        private List<Refuel> refuels = new ArrayList<>();
        private String vId = "all";

        public Adapter(String vId) {
            this.vId = vId;
        }

        public void animateTo(List<Refuel> items) {
            applyAndAnimateRemovals(items);
            applyAndAnimateAdditions(items);
            applyAndAnimateMovedItems(items);
        }

        private void applyAndAnimateRemovals(List<Refuel> newItems) {
            for (int i = refuels.size() - 1; i >= 0; i--) {
                final Refuel model = refuels.get(i);
                if (!newItems.contains(model)) {
                    removeItem(i);
                }
            }
        }

        private void applyAndAnimateAdditions(List<Refuel> newItems) {
            for (int i = 0, count = newItems.size(); i < count; i++) {
                final Refuel model = newItems.get(i);
                if (!refuels.contains(model)) {
                    addItem(i, model);
                }
            }
        }

        private void applyAndAnimateMovedItems(List<Refuel> newItems) {
            for (int toPosition = newItems.size() - 1; toPosition >= 0; toPosition--) {
                final Refuel model = newItems.get(toPosition);
                final int fromPosition = refuels.indexOf(model);
                if (fromPosition >= 0 && fromPosition != toPosition) {
                    moveItem(fromPosition, toPosition);
                }
            }
        }

        public Refuel removeItem(int position) {
            final Refuel model = refuels.remove(position);
            notifyItemRemoved(position);
            return model;
        }

        public void addItem(int position, Refuel item) {
            refuels.add(position, item);
            notifyItemInserted(position);
        }

        public void moveItem(int fromPosition, int toPosition) {
            final Refuel model = refuels.remove(fromPosition);
            refuels.add(toPosition, model);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_refuel, parent, false);

            final Holder holder = new Holder(view);

            holder.itemView.setTag(holder);
            holder.itemView.setOnClickListener(this);

            return holder;
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            final Refuel object = refuels.get(position);

            holder.txtCRAmount.setText(object.cost);
            String odo = (!object.odo.isEmpty()) ? object.odo + " km." : "";
            SpannableString vName;
            try {
                Vehicle vehicle = databaseHelper.vehicle(object.getVehicleId());
                vName = vehicleName(vehicle);
            } catch (Exception e) { e.printStackTrace(); vName = new SpannableString("Vehicle not found!"); }
            holder.txtCRVName.setText((vId.equals("all") || odo.isEmpty()) ? vName : odo);
            holder.txtCRDate.setText(userInterface.date(object.date));
        }

        @Override
        public int getItemCount() {
            return refuels.size();
        }

        @Override
        public void onClick(View v) {
            final Holder holder = (Holder) v.getTag();

            startActivity(new Intent(getActivity(), Retrieve.class).putExtra(Constants.Bundle.ID, refuels.get(holder.getAdapterPosition()).getId()).putExtra(Constants.Bundle.PAGE_CHOICE, CrudChoices.REFUEL));
        }

        private SpannableString vehicleName(Vehicle vehicle) {
            SpannableString title;
            if (vehicle.name == null) {
                if (vehicle.model != null) {
                    String t = vehicle.model.name + ", " + vehicle.model.manu.name + "";
                    title = new SpannableString(t);
                    title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                } else
                    title = new SpannableString(vehicle.reg);
            } else {
                if (vehicle.name.isEmpty()) {
                    if (vehicle.model != null) {
                        String t = vehicle.model.name + ", " + vehicle.model.manu.name + "";
                        title = new SpannableString(t);
                        title.setSpan(new RelativeSizeSpan(0.7f), vehicle.model.name.length(), title.length(), 0);
                    } else
                        title = new SpannableString(vehicle.reg);
                } else
                    title = new SpannableString(vehicle.name);
            }
            return title;
        }
    }

    public class Holder extends RecyclerView.ViewHolder {
        private AppCompatTextView txtCRAmount, txtCRDate, txtCRVName;
        public Holder(View itemView) {
            super(itemView);
            txtCRAmount = (AppCompatTextView) itemView.findViewById(R.id.text_amount);
            txtCRDate = (AppCompatTextView) itemView.findViewById(R.id.text_date);
            txtCRVName = (AppCompatTextView) itemView.findViewById(R.id.text_vehicle_name);

            txtCRAmount.setTypeface(userInterface.font(UserInterface.font.roboto_light));
            txtCRDate.setTypeface(userInterface.font(UserInterface.font.roboto_thin));
            txtCRVName.setTypeface(userInterface.font(UserInterface.font.roboto_light));
        }
    }
}
