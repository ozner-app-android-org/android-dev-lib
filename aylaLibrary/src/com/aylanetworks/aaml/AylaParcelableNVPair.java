//
//  AylaParcelableNVPair.java
//  Ayla Mobile Library
//
//  Created by Daniel Myers on 8/15/12.
//  Copyright (c) 2012 Ayla Networks. All rights reserved.
//

package com.aylanetworks.aaml;

import org.apache.http.NameValuePair;
import android.os.Parcel;
import android.os.Parcelable;

public class AylaParcelableNVPair implements Parcelable
{
	String name, value;

	public AylaParcelableNVPair(String name, String value){
		this.name = name;
		this.value = value;
	}


	public String getName() {
		return name;
	}


	public String getValue() {
		return value;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeString(value);
	}

	// used to regenerate objects. Parcelables must have a CREATOR implementing these two methods
	public static final Parcelable.Creator<AylaParcelableNVPair> CREATOR = new Parcelable.Creator<AylaParcelableNVPair>() {
		public AylaParcelableNVPair createFromParcel(Parcel in) {
			return new AylaParcelableNVPair(in);
		}

		public AylaParcelableNVPair[] newArray(int size) {
			return new AylaParcelableNVPair[size];
		}
	};

	// example constructor that taking a Parcel and returning a populated object
	AylaParcelableNVPair(Parcel in) {
		name = in.readString();
		value = in.readString();
	}
}
