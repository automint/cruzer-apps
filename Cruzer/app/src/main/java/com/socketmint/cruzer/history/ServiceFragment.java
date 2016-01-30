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
import android.util.Log;
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
import com.socketmint.cruzer.dataholder.Service;
import com.socketmint.cruzer.dataholder.Vehicle;
import com.socketmint.cruzer.dataholder.Workshop;
import com.socketmint.cruzer.manage.Constants;
import com.socketmint.cruzer.ui.UiElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ServiceFragment extends Fragment {
    private static final String TAG = "ViewServices";

    private UiElement uiElement;

    private static DatabaseHelper databaseHelper;
    private static Adapter adapter;
    private static String vId;
    private static List<Service> services;
    private static boolean addSuccess = false;

    private Tracker analyticsTracker;

    public static ServiceFragment newInstance(String vId) {
        ServiceFragment fragment = new ServiceFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Bundle.VEHICLE_ID, vId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_service, container, false);

        analyticsTracker = ((CruzerApp) getActivity().getApplication()).getAnalyticsTracker();

        uiElement = new UiElement(getActivity());
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
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler_history_service);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new Adapter(vId);
        recyclerView.setAdapter(adapter);
    }

    public static void addData() {
        addSuccess = false;
        try {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        services = (vId.equals("all")) ? databaseHelper.services() : databaseHelper.services(Collections.singletonList(DatabaseSchema.COLUMN_VEHICLE_ID), new String[]{vId});
                        Collections.sort(services, new Comparator<Service>() {
                            @Override
                            public int compare(Service lhs, Service rhs) {
                                return rhs.date.compareTo(lhs.date);
                            }
                        });
                        addSuccess = true;
                    } catch (Exception e) { Log.d(TAG, "can't add"); }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    if (addSuccess)
                        adapter.animateTo(services);
                    super.onPostExecute(result);
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

        private List<Service> services = new ArrayList<>();
        private String vId = "all";

        public Adapter(String vId) {
            this.vId = vId;
        }

        public void animateTo(List<Service> items) {
            applyAndAnimateRemovals(items);
            applyAndAnimateAdditions(items);
            applyAndAnimateMovedItems(items);
        }

        private void applyAndAnimateRemovals(List<Service> newItems) {
            for (int i = services.size() - 1; i >= 0; i--) {
                final Service model = services.get(i);
                if (!newItems.contains(model)) {
                    removeItem(i);
                }
            }
        }

        private void applyAndAnimateAdditions(List<Service> newItems) {
            for (int i = 0, count = newItems.size(); i < count; i++) {
                final Service model = newItems.get(i);
                if (!services.contains(model)) {
                    addItem(i, model);
                }
            }
        }

        private void applyAndAnimateMovedItems(List<Service> newItems) {
            for (int toPosition = newItems.size() - 1; toPosition >= 0; toPosition--) {
                final Service model = newItems.get(toPosition);
                final int fromPosition = services.indexOf(model);
                if (fromPosition >= 0 && fromPosition != toPosition) {
                    moveItem(fromPosition, toPosition);
                }
            }
        }

        public Service removeItem(int position) {
            final Service model = services.remove(position);
            notifyItemRemoved(position);
            return model;
        }

        public void addItem(int position, Service item) {
            services.add(position, item);
            notifyItemInserted(position);
        }

        public void moveItem(int fromPosition, int toPosition) {
            final Service model = services.remove(fromPosition);
            services.add(toPosition, model);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_service, parent, false);

            final Holder holder = new Holder(view);

            holder.itemView.setTag(holder);
            holder.itemView.setOnClickListener(this);

            return holder;
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            final Service object = services.get(position);

            holder.txtCSAmount.setText(object.cost);
            Workshop workshop = databaseHelper.workshop(Collections.singletonList(DatabaseSchema.COLUMN_ID), new String[]{object.getWorkshopId()});
            String ws = (workshop != null) ? workshop.name : "";
            SpannableString vName;
            try {
                Vehicle vehicle = databaseHelper.vehicle(object.getVehicleId());
                vName = vehicleName(vehicle);
            } catch (Exception e) { e.printStackTrace(); vName = new SpannableString("Vehicle not found!"); }
            holder.txtCSVName.setText((vId.equals("all") || ws.isEmpty()) ? vName : ws);
            holder.txtCSDate.setText(uiElement.date(object.date));
        }

        @Override
        public int getItemCount() {
            return services.size();
        }

        @Override
        public void onClick(View v) {
            final Holder holder = (Holder) v.getTag();

            startActivity(new Intent(getActivity(), Retrieve.class).putExtra(Constants.Bundle.ID, services.get(holder.getAdapterPosition()).getId()).putExtra(Constants.Bundle.PAGE_CHOICE, CrudChoices.SERVICE));
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
        private AppCompatTextView txtCSAmount, txtCSDate, txtCSVName;
        public Holder(View itemView) {
            super(itemView);
            txtCSAmount = (AppCompatTextView) itemView.findViewById(R.id.text_amount);
            txtCSDate = (AppCompatTextView) itemView.findViewById(R.id.text_date);
            txtCSVName = (AppCompatTextView) itemView.findViewById(R.id.text_vehicle_name);
        }
    }
}
