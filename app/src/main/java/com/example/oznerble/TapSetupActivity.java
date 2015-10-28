package com.example.oznerble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import com.example.oznerble.R.id;
import com.example.oznerble.R.layout;
import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.tap.TapSetting;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("SimpleDateFormat")
public class TapSetupActivity extends Activity implements View.OnClickListener {
	OznerBLEBinder service = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(layout.activity_cup_setup);
		super.onCreate(savedInstanceState);

		OznerBLEApplication app = (OznerBLEApplication) this.getApplication();
		service = app.getService();
		if (service == null)
			return;
		String address = getIntent().getStringExtra("Address");
		mTap = service.getTapManager().getTap(address);
		mTapSetting = mTap.Setting();
		setContentView(R.layout.activity_tap_setup);
		tap_Name = (EditText) this.findViewById(id.Tap_Name);
		tap_Address = (TextView) this.findViewById(id.Tap_Address);

		isDetectTime1 = (CheckBox) this.findViewById(id.Tap_DetectTime1_Check);
		DetectTime1 = (TextView) this.findViewById(id.Tap_DetectTime1_Text);
		isDetectTime2 = (CheckBox) this.findViewById(id.Tap_DetectTime2_Check);
		DetectTime2 = (TextView) this.findViewById(id.Tap_DetectTime2_Text);
		isDetectTime3 = (CheckBox) this.findViewById(id.Tap_DetectTime3_Check);
		DetectTime3 = (TextView) this.findViewById(id.Tap_DetectTime3_Text);
		isDetectTime4 = (CheckBox) this.findViewById(id.Tap_DetectTime4_Check);
		DetectTime4 = (TextView) this.findViewById(id.Tap_DetectTime4_Text);

		this.findViewById(id.Tap_DetectTime1Panel).setOnClickListener(this);
		this.findViewById(id.Tap_DetectTime2Panel).setOnClickListener(this);
		this.findViewById(id.Tap_DetectTime3Panel).setOnClickListener(this);
		this.findViewById(id.Tap_DetectTime4Panel).setOnClickListener(this);
		this.findViewById(id.Submit).setOnClickListener(this);
		
		load();
	}

	Tap mTap = null;
	TapSetting mTapSetting = null;
	EditText tap_Name;
	TextView tap_Address;

	CheckBox isDetectTime1;
	TextView DetectTime1;
	Date d1;

	CheckBox isDetectTime2;
	TextView DetectTime2;
	Date d2;

	CheckBox isDetectTime3;
	TextView DetectTime3;
	Date d3;

	CheckBox isDetectTime4;
	TextView DetectTime4;
	Date d4;

	@SuppressWarnings("deprecation")
	private void load() {
		if (mTap != null) {
			tap_Name.setText(mTapSetting.name());
			tap_Address.setText(mTap.Address());

			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");

			isDetectTime1.setChecked(mTapSetting.isDetectTime1());
			d1 = new Date(0, 0, 0, mTap.Setting().DetectTime1() / 3600, mTap
					.Setting().DetectTime1() % 3600 / 60);
			DetectTime1.setText(fmt.format(d1));

			isDetectTime2.setChecked(mTapSetting.isDetectTime2());
			d2 = new Date(0, 0, 0, mTap.Setting().DetectTime2() / 3600, mTap
					.Setting().DetectTime2() % 3600 / 60);
			DetectTime2.setText(fmt.format(d2));

			isDetectTime3.setChecked(mTapSetting.isDetectTime3());
			d3 = new Date(0, 0, 0, mTap.Setting().DetectTime3() / 3600, mTap
					.Setting().DetectTime3() % 3600 / 60);
			DetectTime3.setText(fmt.format(d3));

			isDetectTime4.setChecked(mTapSetting.isDetectTime4());
			d4 = new Date(0, 0, 0, mTap.Setting().DetectTime4() / 3600, mTap
					.Setting().DetectTime4() % 3600 / 60);
			DetectTime4.setText(fmt.format(d4));

		}
	}

	private void showErrorMessage(String message) {
		new AlertDialog.Builder(this).setTitle("错误").setMessage(message)
				.setPositiveButton("确定", null).show();
	}

	private void Submit() {
		String Name = tap_Name.getText().toString();
		if (Name.isEmpty()) {
			showErrorMessage("名称不能为空");
			return;
		}
		try {

			mTapSetting.name(tap_Name.getText().toString());
			mTapSetting.isDetectTime1(isDetectTime1.isChecked());
			mTapSetting
					.DetectTime1(d1.getHours() * 3600 + d1.getMinutes() * 60);

			mTapSetting.isDetectTime2(isDetectTime2.isChecked());
			mTapSetting
					.DetectTime2(d2.getHours() * 3600 + d2.getMinutes() * 60);

			mTapSetting.isDetectTime3(isDetectTime3.isChecked());
			mTapSetting
					.DetectTime3(d3.getHours() * 3600 + d3.getMinutes() * 60);

			mTapSetting.isDetectTime4(isDetectTime4.isChecked());
			mTapSetting
					.DetectTime4(d4.getHours() * 3600 + d4.getMinutes() * 60);

			service.getDeviceManager().save(mTap);
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
		case id.Tap_DetectTime1Panel: {
			final TimePicker picker = new TimePicker(this);
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
			picker.setIs24HourView(true);
			picker.setOnTimeChangedListener(new OnTimeChangedListener() {
				@Override
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
				}
			});

			picker.setCurrentHour(d1.getHours());
			picker.setCurrentMinute(d1.getMinutes());

			new AlertDialog.Builder(this).setTitle("").setView(picker)
					.setPositiveButton("确定", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
							d1 = new Date(0, 0, 0, picker.getCurrentHour(),
									picker.getCurrentMinute());
							DetectTime1.setText(fmt.format(d1));
						}
					}).show();
		}
			break;
		case id.Tap_DetectTime2Panel: {
			final TimePicker picker = new TimePicker(this);
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
			picker.setIs24HourView(true);
			picker.setOnTimeChangedListener(new OnTimeChangedListener() {
				@Override
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
				}
			});

			picker.setCurrentHour(d2.getHours());
			picker.setCurrentMinute(d2.getMinutes());

			new AlertDialog.Builder(this).setTitle("").setView(picker)
					.setPositiveButton("确定", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
							d2 = new Date(0, 0, 0, picker.getCurrentHour(),
									picker.getCurrentMinute());
							DetectTime2.setText(fmt.format(d2));
						}
					}).show();
		}
			break;
		case id.Tap_DetectTime3Panel: {
			final TimePicker picker = new TimePicker(this);
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
			picker.setIs24HourView(true);
			picker.setOnTimeChangedListener(new OnTimeChangedListener() {
				@Override
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
				}
			});

			picker.setCurrentHour(d3.getHours());
			picker.setCurrentMinute(d3.getMinutes());

			new AlertDialog.Builder(this).setTitle("").setView(picker)
					.setPositiveButton("确定", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
							d3 = new Date(0, 0, 0, picker.getCurrentHour(),
									picker.getCurrentMinute());
							DetectTime3.setText(fmt.format(d3));
						}
					}).show();
		}
			break;
		case id.Tap_DetectTime4Panel: {
			final TimePicker picker = new TimePicker(this);
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
			picker.setIs24HourView(true);
			picker.setOnTimeChangedListener(new OnTimeChangedListener() {
				@Override
				public void onTimeChanged(TimePicker view, int hourOfDay,
						int minute) {
				}
			});

			picker.setCurrentHour(d4.getHours());
			picker.setCurrentMinute(d4.getMinutes());

			new AlertDialog.Builder(this).setTitle("").setView(picker)
					.setPositiveButton("确定", new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
							d4 = new Date(0, 0, 0, picker.getCurrentHour(),
									picker.getCurrentMinute());
							DetectTime4.setText(fmt.format(d4));
						}
					}).show();
		}
			break;
		}
	}
}
