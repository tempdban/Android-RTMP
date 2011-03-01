/*
 * Camundo <http://www.camundo.com> Copyright (C) 2011  Wouter Van der Beken.
 *
 * This file is part of Camundo.
 *
 * Camundo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Camundo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Camundo.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.camundo.media;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;



public class FFMPEGRtmpSubscriber extends Thread{

	private static final String TAG = "FFMPEGRtmpSubscriber";
	
	private FFMPEGOutputPipe pipe;
	private AudioTrack audioTrack;
	 

    public FFMPEGRtmpSubscriber( FFMPEGOutputPipe pipe ) {
    	this.pipe = pipe;
    }
    
        
    @Override
    public void run() {
        try {
        	//hmmm can always try
        	pipe.setPriority(MAX_PRIORITY);
            pipe.start();
            
            while( !pipe.processRunning() ) {
            	Log.i( TAG, "[ run() ] pipe not yet running, waiting.");
            	try {
            		Thread.sleep(1000);
            	}
            	catch( Exception e) {
            		e.printStackTrace();
            	}
            }
            
            int minBufferSize = AudioTrack.getMinBufferSize(AudioCodec.PCM_S16LE.RATE_11025, 
            													AudioFormat.CHANNEL_CONFIGURATION_MONO, 
            													AudioFormat.ENCODING_PCM_16BIT) * 2;
            
            audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 
						            		AudioCodec.PCM_S16LE.RATE_11025, 
						            		AudioFormat.CHANNEL_CONFIGURATION_MONO, 
						            		AudioFormat.ENCODING_PCM_16BIT, 
						            		minBufferSize * 4, 
						            		AudioTrack.MODE_STREAM);
            
            ByteBuffer buffer = ByteBuffer.allocate(minBufferSize);
		    buffer.order(ByteOrder.LITTLE_ENDIAN);
		    
            Log.d( TAG, "buffer length [" + minBufferSize + "]");
        	int len;
            
            //wait until minimum amount of data ( header is 44 )
            pipe.bootstrap( 100 );
            
            int overallBytes = 0;
            boolean started = false;
            
           	while( (len = pipe.read(buffer.array(), buffer.arrayOffset(), buffer.capacity())) > 0 ) {
           		//Log.d(NAME, "[ run() ] len [" + len + "] buffer empty [" + pipe.available() + "]" );
           		overallBytes+= audioTrack.write(buffer.array(), 0, len);
           		//audioTrack.flush();
           		if (!started && overallBytes > minBufferSize ){
           			audioTrack.setPlaybackHeadPosition(2);
           			audioTrack.play();
                    started = true;
                }
           	}
            
        } 
        catch (IOException e) {
        	e.printStackTrace();
            Log.e(getClass().getName(), e.getMessage());
        }
        Log.i( TAG, "[ run() ] done");
    }
    
    
    public void shutdown(){
    	Log.i( TAG , "[ shutdown() ] up is false");
    	pipe.close();
    	audioTrack.flush();
        audioTrack.stop();
        audioTrack.release();
        
    }

    
}