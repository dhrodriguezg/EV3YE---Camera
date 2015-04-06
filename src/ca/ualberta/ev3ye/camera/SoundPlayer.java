package ca.ualberta.ev3ye.camera;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundPlayer {
	
	private MediaPlayer mediaLightOn = null;
	private MediaPlayer mediaLightOff = null;
	private MediaPlayer mediaCameraOffline = null;
	private MediaPlayer mediaCameraOnline = null;
	private MediaPlayer mediaControllerOffline = null;
	private MediaPlayer mediaControllerOnline = null;
	private MediaPlayer mediaEV3Online = null;
	private MediaPlayer mediaEV3Offline = null;
	private MediaPlayer mediaEV3Down = null;
	private MediaPlayer mediaEV3Down2 = null;
	
	public SoundPlayer(Context context){
		
		mediaLightOn = MediaPlayer.create(context , R.raw.light_on);
		mediaLightOff = MediaPlayer.create(context , R.raw.light_off);
		mediaCameraOffline = MediaPlayer.create(context , R.raw.camera_offline);
		mediaCameraOnline = MediaPlayer.create(context , R.raw.camera_online);
		mediaControllerOffline = MediaPlayer.create(context , R.raw.controller_offline);
		mediaControllerOnline = MediaPlayer.create(context , R.raw.controller_online);
		mediaEV3Online = MediaPlayer.create(context , R.raw.ev3_online);
		mediaEV3Offline = MediaPlayer.create(context , R.raw.ev3_offline);
		mediaEV3Down = MediaPlayer.create(context , R.raw.ev3_down);
		mediaEV3Down2 = MediaPlayer.create(context , R.raw.ev3_down2);
	}
	
	public void lightOn(){
		mediaLightOn.start();
	}
	
	public void lightOff(){
		mediaLightOff.start();
	}
	
	public void cameraOffline(){
		mediaCameraOffline.start();
	}
	
	public void cameraOnline(){
		mediaCameraOnline.start();
	}
	
	public void controllerOnline(){
		mediaControllerOnline.start();
	}
	
	public void controllerOffline(){
		mediaControllerOffline.start();
	}
	
	public void ev3Online(){
		mediaEV3Online.start();
	}
	
	public void ev3Offline(){
		mediaEV3Offline.start();
	}
	
	public void ev3Down(){
		mediaEV3Down.start();
	}
	
	public void ev3Down2(){
		mediaEV3Down2.start();
	}
}
