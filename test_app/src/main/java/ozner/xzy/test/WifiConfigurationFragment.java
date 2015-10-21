package ozner.xzy.test;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WifiConfigurationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WifiConfigurationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WifiConfigurationFragment extends Fragment {
    private static final String SSID_Key = "SSID";
    private static final String Password_Key = "Password";

    private String SSID;
    private String Password;

    public static WifiConfigurationFragment newInstance(String SSID, String Password) {
        WifiConfigurationFragment fragment = new WifiConfigurationFragment();
        Bundle args = new Bundle();
        args.putString(SSID, SSID);
        args.putString(Password, Password);
        fragment.setArguments(args);
        return fragment;
    }

    public WifiConfigurationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            SSID = getArguments().getString(SSID_Key);
            Password = getArguments().getString(Password_Key);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wifi_configuration, container, false);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


}
