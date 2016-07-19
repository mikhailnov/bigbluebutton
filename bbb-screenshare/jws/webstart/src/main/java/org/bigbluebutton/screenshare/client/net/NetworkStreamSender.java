/**
 * BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
 * 
 * Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.bigbluebutton.screenshare.client.net;

import java.util.Timer;
import java.util.TimerTask;
import org.bigbluebutton.screenshare.client.ExitCode;

public class NetworkStreamSender implements NetworkStreamListener {	
  public static final String NAME = "NETWORKSTREAMSENDER: ";

  private final String meetingId;
  private final String streamId;
  private NetworkHttpStreamSender httpSenders;
  private NetworkConnectionListener listener;
  private final SequenceNumberGenerator seqNumGenerator = new SequenceNumberGenerator();
  private String host = "192.168.23.22";

  private TimerTask timerTask = new UpdateTimerTask();
  private Timer timer = new Timer();

  public NetworkStreamSender(String host, String meetingId, String streamId) {
    this.meetingId = meetingId;
    this.streamId = streamId;  
    this.host = host; 
    connect();
  }

  public void addNetworkConnectionListener(NetworkConnectionListener listener) {
    this.listener = listener;
  }

  private void notifyNetworkConnectionListener(ExitCode reason) {
    if (listener != null) listener.networkConnectionException(reason);
  }

  private boolean connect() {
    httpSenders = new NetworkHttpStreamSender(meetingId, streamId, seqNumGenerator);
    httpSenders.addListener(this);
    try {
      httpSenders.connect(host);
    } catch (ConnectionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return true;
  }


  public void stopSharing() {
    System.out.println("Queueing ShareStoppedMessage");
    send(new ShareStoppedMessage(meetingId, streamId));
  }

  public void startSharing(int width, int height) {
    System.out.println("Queueing ShareStartedMessage");
    send(new ShareStartedMessage(meetingId, streamId, width, height));
  }

  private void send(Message message) {
    httpSenders.send(message);
  }

  public void start() {
    //System.out.println(NAME + "Starting network sender.");
    httpSenders.start();
    timer.scheduleAtFixedRate(timerTask, 0, 2 * 1000);
  }

  public void stop() throws ConnectionException {
    timer.cancel();

    if (httpSenders != null) {
      httpSenders.disconnect();
      httpSenders.stop();      
    }

  }


  @Override
  public void networkException(ExitCode reason) {
    try {						
      System.out.println(NAME + "NetworkException - " + reason.getExitCode());
      stop();
      notifyNetworkConnectionListener(reason);
    } catch (ConnectionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }		
  }

  private class UpdateTimerTask extends TimerTask {
    @Override
    public void run() {
      System.out.println("Queueing ShareUpdateMessage");
      send(new ShareUpdateMessage(meetingId, streamId));
    }  
  }

}
