package com.example.oznerble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import com.example.oznerble.R.id;
import com.example.oznerble.R.layout;
import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.cup.Cup;
import com.ozner.cup.CupSetting;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("SimpleDateFormat")
public class CupSetupActivity extends Activity implements View.OnClickListener {
	OznerBLEBinder service=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(layout.activity_cup_setup);
		super.onCreate(savedInstanceState);
		
		OznerBLEApplication app=(OznerBLEApplication)this.getApplication();
		service=app.getService();
		if (service==null) return;
		String address=getIntent().getStringExtra("Address");

		mCup=service.getCupManager().getCup(address);
		mCupSetting = (CupSetting) mCup.Setting();
		setContentView(R.layout.activity_cup_setup);
		cup_Name = (EditText) this.findViewById(id.Cup_Name);
		cup_Address = (TextView) this.findViewById(id.Cup_Address);
		cup_Remind = (CheckBox) this.findViewById(id.Cup_Remind);
		cup_RemindStart = (TextView) this.findViewById(id.Cup_RemindStart);
		cup_RemindEnd = (TextView) this.findViewById(id.Cup_RemindEnd);
		cup_RemindInterval = (EditText) this
				.findViewById(id.Cup_RemindInterval);
		cup_HaloColor = (Button) this.findViewById(id.Cup_HaloColor);
		cup_HaloCounter = (EditText) this.findViewById(id.Cup_HaloCounter);
		cup_TDS = (RadioButton) this.findViewById(id.Cup_TDS);
		cup_Temp = (RadioButton) this.findViewById(id.Cup_Temp);

		cup_BeepMode_double = (RadioButton) this
				.findViewById(id.Cup_BeepDouble);
		cup_BeepMode_none = (RadioButton) this.findViewById(id.Cup_BeepOnce);
		cup_BeepMode_once = (RadioButton) this.findViewById(id.Cup_BeepNone);

		cup_Halo_Breathe = (RadioButton) this.findViewById(id.Cup_Halo_Breathe);
		cup_Halo_Fast = (RadioButton) this.findViewById(id.Cup_Halo_Fast);
		cup_Halo_Slow = (RadioButton) this.findViewById(id.Cup_Halo_Slow);
		cup_Halo_None = (RadioButton) this.findViewById(id.Cup_Halo_None);
		this.findViewById(id.Submit).setOnClickListener(this);
		this.findViewById(id.SensorZero).setOnClickListener(this);
		
		this.findViewById(id.Cup_RemindStartPanel).setOnClickListener(this);
		this.findViewById(id.Cup_RemindEndPanel).setOnClickListener(this);
		this.findViewById(id.Cup_HaloColorPanel).setOnClickListener(this);
		this.findViewById(id.Cup_HaloColor).setOnClickListener(this);
		load();
	}

	Cup mCup = null;
	CupSetting mCupSetting = null;
	public static final String RemindFragment_Key = "CupRemindSetup";
	EditText cup_Name;
	TextView cup_Address;
	CheckBox cup_Remind;
	TextView cup_RemindStart;
	TextView cup_RemindEnd;
	EditText cup_RemindInterval;
	RadioButton cup_Halo_Breathe;
	RadioButton cup_Halo_Fast;
	RadioButton cup_Halo_Slow;
	RadioButton cup_Halo_None;
	Date StartTime;
	Date EndTime;
	int haloColor;
	Button cup_HaloColor;
	EditText cup_HaloCounter;
	RadioButton cup_TDS;
	RadioButton cup_Temp;
	RadioButton cup_BeepMode_none;
	RadioButton cup_BeepMode_once;
	RadioButton cup_BeepMode_double;

	@SuppressWarnings("deprecation")
	private void load() {
		if (mCup != null) {
			cup_Name.setText(mCupSetting.name());
			cup_Address.setText(mCup.Address());
			cup_Remind.setChecked(mCupSetting.RemindEnable());
			SimpleDateFormat fmt = new SimpleDateFormat("hh:mm");
			StartTime = new Date(0, 0, 0, mCup.Setting().remindStart() / 3600,
					mCup.Setting().remindStart() % 3600 / 60);
			EndTime = new Date(0, 0, 0, mCup.Setting().remindEnd() / 3600, mCup
					.Setting().remindEnd() % 3600 / 60);

			cup_RemindStart.setText(fmt.format(StartTime));
			cup_RemindEnd.setText(fmt.format(EndTime));

			cup_RemindInterval.setText(String.valueOf( mCupSetting.remindInterval()));
			haloColor=mCupSetting.haloColor();
			cup_HaloColor.setBackgroundColor(mCupSetting.haloColor());
			cup_HaloCounter.setText(String.valueOf(mCupSetting.haloConter()));

			cup_TDS.setChecked(mCupSetting.haloMode() == CupSetting.Halo_TDS ? true
					: false);
			cup_Temp.setChecked(mCupSetting.haloMode() == CupSetting.Halo_Temperature ? true
					: false);
			cup_BeepMode_double
					.setChecked(mCupSetting.beepMode() == CupSetting.Beep_Dobule ? true
							: false);
			cup_BeepMode_none
					.setChecked(mCupSetting.beepMode() == CupSetting.Beep_Nono ? true
							: false);
			cup_BeepMode_once
					.setChecked(mCupSetting.beepMode() == CupSetting.Beep_Once ? true
							: false);
			cup_Halo_Breathe
					.setChecked(mCupSetting.haloSpeed() == CupSetting.Halo_Breathe ? true
							: false);
			cup_Halo_Fast
					.setChecked(mCupSetting.haloSpeed() == CupSetting.Halo_Fast ? true
							: false);
			cup_Halo_Slow
					.setChecked(mCupSetting.haloSpeed() == CupSetting.Halo_Slow ? true
							: false);
			cup_Halo_None
					.setChecked(mCupSetting.haloSpeed() == CupSetting.Halo_None ? true
							: false);

		}
	}

	private void showErrorMessage(String message) {
		new AlertDialog.Builder(this).setTitle("错误").setMessage(message)
				.setPositiveButton("确定", null).show();
	}

	private void Submit() {
		String Name = cup_Name.getText().toString();
		if (Name.isEmpty()) {
			showErrorMessage("名称不能为空");
			return;
		}
		try {
			int inv = Integer.parseInt(cup_RemindInterval.getText().toString());
			if (inv <= 0) {
				showErrorMessage("间隔太小");
				return;
			}

			int counter = Integer
					.parseInt(cup_HaloCounter.getText().toString());
			if (counter <= 0) {
				showErrorMessage("闪烁次数太小");
				return;
			}
			if (StartTime.equals(EndTime)) {
				showErrorMessage("起始结束时间一样");
				return;
			}
			mCupSetting.name(cup_Name.getText().toString());
			mCupSetting.RemindEnable(cup_Remind.isChecked());
			mCupSetting.remindStart(StartTime.getHours() * 3600
					+ StartTime.getMinutes() * 60);
			mCupSetting.remindEnd(EndTime.getHours() * 3600
					+ EndTime.getMinutes() * 60);
			mCupSetting.remindInterval(inv);

			mCupSetting.haloMode(cup_TDS.isChecked() ? CupSetting.Halo_TDS
					: CupSetting.Halo_Temperature);
			mCupSetting.haloColor(haloColor);
			mCupSetting.haloConter(counter);
			int beep = CupSetting.Beep_Once;
			if (cup_BeepMode_double.isChecked())
				beep = CupSetting.Beep_Dobule;
			if (cup_BeepMode_once.isChecked())
				beep = CupSetting.Beep_Once;
			if (cup_BeepMode_none.isChecked())
				beep = CupSetting.Beep_Nono;
			mCupSetting.beepMode(beep);
			service.getDeviceManager().save(mCup);
			this.finish();
		} catch (Exception e) {
			showErrorMessage(e.toString());
			return;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case id.Submit:
			this.Submit();
			break;

			case id.Cup_RemindStartPanel: {
			final TimePicker picker = new TimePicker(this);

			SimpleDateFormat fmt = new SimpleDateFormat("hh:mm");
			picker.setIs24HourView(true);
			picker.setOnTimeChangedListener(new OnTimeChangedListener() {
				@Override
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
				}
			});

			picker.setCurrentHour(StartTime.getHours());
			picker.setCurrentMinute(StartTime.getMinutes());

			new AlertDialog.Builder(this).setTitle("").setView(picker)
					.setPositiveButton("确定", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SimpleDateFormat fmt = new SimpleDateFormat("hh:mm");
							StartTime = new Date(0, 0, 0, picker
									.getCurrentHour(), picker
									.getCurrentMinute());
							cup_RemindStart.setText(fmt.format(StartTime));
						}
					}).show();
		}
			break;
		case id.Cup_RemindEndPanel: {
			final TimePicker picker = new TimePicker(this);
			SimpleDateFormat fmt = new SimpleDateFormat("hh:mm");
			picker.setIs24HourView(true);
			picker.setOnTimeChangedListener(new OnTimeChangedListener() {
				@Override
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
				}
			});

			picker.setCurrentHour(EndTime.getHours());
			picker.setCurrentMinute(EndTime.getMinutes());

			new AlertDialog.Builder(this).setTitle("").setView(picker)
					.setPositiveButton("确定", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SimpleDateFormat fmt = new SimpleDateFormat("hh:mm");
							EndTime = new Date(0, 0, 0,
									picker.getCurrentHour(), picker
											.getCurrentMinute());
							cup_RemindEnd.setText(fmt.format(EndTime));
						}
					}).show();
		}
			break;
		case id.Cup_HaloColor:
		case id.Cup_HaloColorPanel: {
			ColorPickerBox box = new ColorPickerBox(this, haloColor);
			box.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					haloColor = ((ColorPickerBox) dialog).getColor();
					cup_HaloColor.setBackgroundColor(haloColor);
				}
			});
			box.show();
		}
			break;
		}
	}
}
