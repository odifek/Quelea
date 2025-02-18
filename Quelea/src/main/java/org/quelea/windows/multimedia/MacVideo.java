/*
 * This file is part of Quelea, free projection software for churches.
 * 
 * Copyright (C) 2012 Michael Berry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.quelea.windows.multimedia;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quelea.services.utils.LoggerUtils;
import org.quelea.services.utils.QueleaProperties;
import org.quelea.services.utils.Utils;
import org.quelea.windows.main.QueleaApp;

/**
 * A native AVPlayer window which is responsible for moving where it's told, and
 * playing video files. Transparent windows can then sit on top of this giving
 * the impression of a video background. This is a singleton to follow the
 * VLCWindow interface.
 * <p>
 * @author grgarno
 */
public class MacVideo extends VLCWindow {

    private static final ExecutorService MAC_VID_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Logger LOGGER = LoggerUtils.getLogger();
    protected static final VLCWindow MAC_INSTANCE = new MacVideo();
    private boolean hideButton;
    private boolean show;
    private boolean paused;
    private volatile boolean init;
    private String location;
    private volatile double hue = 0;
    private String lastPath = "";
    private String lastOptions = "";
    private boolean lastStretch = false;

    /**
     * Constructor of this class
     */
    private MacVideo() {
        runOnVIDThread(() -> {
            try {
                // Wait up to 10 seconds for AVPlayerJava native code to get loaded
                int timeout = 10000;
                // 50ms between each test of whether native code is loaded
                int sleep_interval = 50;

                // Loop until native code has loaded or timed out
                while( !AVPlayerJava.isModuleLoaded() && timeout > 0 ) {
                    TimeUnit.MILLISECONDS.sleep(sleep_interval);
                    timeout -= sleep_interval;
                }

                if( timeout == 0 ){
                    LOGGER.log(Level.WARNING, "Couldn't initialise video, load of native library timed out");
                } else {
                    init = AVPlayerJava.isInit();
                    LOGGER.log(Level.INFO, "Video initialised ok");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Couldn't initialise video, good luck as to why!", ex);
            }
        });

    }

    /**
     * Determine if AVPlayer has initialised correctly.
     * <p>
     * @return true if it has, false if it hasn't because something went wrong
     */
    @Override
    public boolean isInit() {
        runOnVIDThreadAndWait(() -> {
            //Just to block until construction has finished!
        });
        return init;
    }

    /**
     * Sets whether the video should repeat.
     *
     * @param repeat True to repeat, false otherwise
     */
    @Override
    public void setRepeat(final boolean repeat) {
        runOnVIDThread(() -> {
//                System.out.println("setRepeat() start");
            if (init) {
                AVPlayerJava.setRepeat(repeat);
            }
//                System.out.println("setRepeat() end");
        });
    }

    /**
     * Load a video into the video player
     *
     * @param path The path to the video on the FS
     * @param options Options passed to the AVPlayer library. (As of release, no
     * options are available)
     * @param stretch Whether the video should be stretched over the frame
     */
    @Override
    public void load(final String path, final String options, final boolean stretch) {
        runOnVIDThread(() -> {
//                System.out.println("load("+path+") start");
            if (init) {
                paused = false;

                String sanitisedPath = path;
                sanitisedPath = sanitisedPath.trim();
                if (sanitisedPath.startsWith("www")) {
                    sanitisedPath = "http://" + sanitisedPath;
                }
                if (AVPlayerJava.isPlaying()) {
                    AVPlayerJava.stop();
                }
                if (options != null) {
                    if (options.length() > 0) {
                        AVPlayerJava.setOptions(options);
                    }
                }

                lastPath = sanitisedPath;
                lastOptions = options;
                lastStretch = stretch;
                AVPlayerJava.setStretch(stretch);
                AVPlayerJava.loadVid(sanitisedPath);

            }
//                System.out.println("load() end");
        });
    }

    /**
     * Play the currently loaded video
     */
    @Override
    public void play() {
        runOnVIDThread(() -> {
//                System.out.println("play() start");
            if (init) {
                AVPlayerJava.setFadeSpeed(QueleaProperties.get().getLogoFadeDuration() / 1000);
                paused = false;
                AVPlayerJava.play();

            }
//                System.out.println("play() end");
        });
    }

    /**
     * Play the video that is passed.
     *
     * @param vid The path to the video on the fs.
     * @param options Options passed to the AVPlayer library. (As of release, no
     * options are available)
     * @param stretch Whether the video should be stretched over the frame
     */
    @Override
    public void play(final String vid, final String options, final boolean stretch) {
        this.location = vid;
        runOnVIDThread(() -> {
//                System.out.println("play("+vid+") start");
            if (init) {
                paused = false;
                if (AVPlayerJava.isPlaying()) {
                    AVPlayerJava.stop();
                }
                if (options != null) {
                    if (options.length() > 0) {
                        AVPlayerJava.setOptions(options);
                    }
                }
                lastPath = vid;
                lastOptions = options;
                lastStretch = stretch;
                AVPlayerJava.setStretch(stretch);
                AVPlayerJava.loadVid(vid);
                AVPlayerJava.setFadeSpeed(QueleaProperties.get().getLogoFadeDuration() / 1000);
                AVPlayerJava.play();

            }
//                System.out.println("play(arg) end");
        });
    }

    /**
     * Get the last location played
     *
     * @return The last location on the fs.
     */
    @Override
    public String getLastLocation() {
        return location;
    }

    /**
     * Pause the currently playing video
     */
    @Override
    public void pause() {
        runOnVIDThread(() -> {
//                System.out.println("pause() start");
            if (init) {
                paused = true;
                AVPlayerJava.pauseVideo();
            }
//                System.out.println("pause() end");
        });
    }

    /**
     * Stop the currently playing video
     */
    @Override
    public void stop() {
        location = null;
        runOnVIDThread(() -> {
//                System.out.println("stop() start");
            if (init) {
                paused = false;
                AVPlayerJava.stop();
            }
//                System.out.println("stop() end");
        });
    }

    private boolean muteTemp;

    /**
     * Returns whether the video player is muted
     *
     * @return True if muted, false otherwise
     */
    @Override
    public boolean isMute() {
        runOnVIDThreadAndWait(() -> {
//                System.out.println("isMute() start");
            if (init) {
                muteTemp = AVPlayerJava.isMute();
            } else {
                muteTemp = false;
            }
//                System.out.println("isMute() end");
        });
        return muteTemp;
    }

    /**
     * Set the mute for the currently playing video
     *
     * @param mute True if should be muted, false otherwise
     */
    @Override
    public void setMute(final boolean mute) {
        runOnVIDThread(() -> {
//                System.out.println("setMute() start");
            if (init) {
                AVPlayerJava.setMute(mute);
            }
//                System.out.println("setMute() end");
        });
    }
    private double progressTemp;

    /**
     * Gets the progress of the video
     *
     * @return A scale from 0.0 to 1.0. 1.0 being fully played.
     */
    @Override
    public double getProgressPercent() {
        runOnVIDThreadAndWait(() -> {
//                System.out.println("getProgressPercent() start");
            if (init) {
                progressTemp = AVPlayerJava.getProgressPercent();
            } else {
                progressTemp = 0;
            }
//                System.out.println("getProgressPercent() end");
        });
        return progressTemp;
    }

    /**
     * Sets the progress of the video
     *
     * @param percent A scale from 0.0 to 1.0. 1.0 being fully played.
     */
    @Override
    public void setProgressPercent(final double percent) {
        runOnVIDThread(() -> {
//                System.out.println("setProgressPercent() start");
            if (init) {
                AVPlayerJava.setProgressPercent(percent);
            }
//                System.out.println("setProgressPercent() end");
        });
    }
    private boolean isPlayingTemp;

    /**
     * See if the video is playing
     *
     * @return True if playing, false otherwise
     */
    @Override
    public boolean isPlaying() {
        runOnVIDThreadAndWait(() -> {
//                System.out.println("isPlaying() start");
            if (init) {
                isPlayingTemp = AVPlayerJava.isPlaying();
            } else {
                isPlayingTemp = false;
            }
//                System.out.println("isPlaying() end");
        });
        return isPlayingTemp;
    }
    private boolean isPausedTemp;

    /**
     * See if the player is paused
     *
     * @return True if paused, false otherwise
     */
    @Override
    public boolean isPaused() {
        runOnVIDThreadAndWait(() -> {
//                System.out.println("isPaused() start");
            if (init) {
                isPausedTemp = paused;
            } else {
                isPausedTemp = false;
            }
//                System.out.println("isPaused() end");
        });
        return isPausedTemp;
    }

    /**
     * Set a runnable to be completed on finish of the currently playing video.
     * WARNING! Not implemented for AVPLayer.
     *
     * @param onFinished The runnable to be run, if this method worked.
     */
    @Override
    public void setOnFinished(final Runnable onFinished) {
        runOnVIDThread(() -> {
//                System.out.println("setOnFinished() start");
            if (init) {
                paused = false;
                //TODO: Monitor code value AVPlayerJava.isFinished();
            }
//                System.out.println("setOnFinished() end");
        });
    }

    /**
     * Show the AVPlayer
     */
    @Override
    public void show() {
        runOnVIDThread(() -> {
//                System.out.println("show() start");
            if (init) {
                show = true;
                updateState();
            }
//                System.out.println("show() end");
        });
    }

    /**
     * Hide the AVPlayer
     */
    @Override
    public void hide() {
        runOnVIDThread(() -> {
//                System.out.println("hide() start");
            if (init) {
                show = false;
                updateState();
            }
//                System.out.println("hide() end");
        });
    }

    /**
     * Set the hide button
     *
     * @param hide True if hidden, false otherwise.
     */
    @Override
    public void setHideButton(final boolean hide) {
        runOnVIDThread(() -> {
//                System.out.println("setHideButton() start");
            if (init) {
                hideButton = hide;
                updateState();
            }
//                System.out.println("setHideButton() end");
        });
    }

    /**
     * Update the state of the playback window.
     */
    private void updateState() {
        runOnVIDThread(() -> {
//                System.out.println("updateState() start");
            if (init) {
                AVPlayerJava.setVisible((!hideButton && show));

            }
//                System.out.println("updateState() end");
        });
    }

    /**
     * Set the location of the playback window
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    @Override
    public void setLocation(final int x, final int y) {
        runOnVIDThread(() -> {
//                System.out.println("setLocation() start");
            if (init) {
                AVPlayerJava.setLocation(x, y);

            }
//                System.out.println("setLocation() end");
        });
    }

    /**
     * Set the size of the AV Player.
     *
     * @param width The width of the window
     * @param height The height of the window
     */
    @Override
    public void setSize(final int width, final int height) {
        runOnVIDThread(() -> {
//                System.out.println("setsize() start");
            if (init) {
                AVPlayerJava.setSize(width, height);

            }
//                System.out.println("setsize() end");
        });
    }
    private int tempX, tempY, tempWidth, tempHeight;
    private boolean showing;

    /**
     * Refresh the position of the AVPlayer window
     */
    @Override
    public void refreshPosition() {
        Utils.fxRunAndWait(() -> {
            showing = QueleaApp.get().getProjectionWindow().isShowing();
            if (showing) {
                tempX = (int) QueleaApp.get().getProjectionWindow().getX();
                tempY = (int) QueleaApp.get().getProjectionWindow().getY();
                tempWidth = (int) QueleaApp.get().getProjectionWindow().getWidth();
                tempHeight = (int) QueleaApp.get().getProjectionWindow().getHeight();
            }
        });
        runOnVIDThread(() -> {
//                System.out.println("refreshPosition() start");
            if (init) {
                if (showing) {
                    show();
                    setLocation(tempX, tempY);
                    setSize(tempWidth, tempHeight);
                } else {
                    hide();
                }
            }
//                System.out.println("refreshPosition() end");
        });
    }

    /**
     * Fade the hue of the player.
     *
     * @param hue The hue to be faded to.
     */
    @Override
    public synchronized void fadeHue(final double hue) {
        /**
         * fading is done when a new video is played. Since the native code only
         * supports setting hue BEFORE the video is played, we set the hue, and
         * play the video again.
         */
        setHue(hue);
        play(lastPath, lastOptions, lastStretch);

    }

    /**
     * Set the hue of the player. Only works if called before the player is
     * loaded
     *
     * @param hue The hue of the video that should be displayed.
     */
    @Override
    public void setHue(final double hue) {
        runOnVIDThread(() -> {
//                System.out.println("set hue start");
            if (init) {
                //input hue, 0-1, AVPlayer expecting -pi to pi
                double passHue;  //0-pi
                if (hue > 0.5) {//should be 0(1) - pi(0.5)
                    passHue = ((1 - hue) * 2) * (Math.PI);
                } else {//should be 0(0) - - pi(0.5)
                    passHue = (hue * 2) * (Math.PI * -1);
                }

                AVPlayerJava.setHue(passHue);
                // System.out.println("Hue: " + hue);
                // System.out.println("Passed Hue: "+passHue);
                MacVideo.this.hue = hue;
            }
//                System.out.println("set hue end");
        });

    }

    /**
     * Get the hue of the currently playing video
     *
     * @return The hue.
     */
    @Override
    public double getHue() {
        return hue;
    }

    /**
     * TODO: Get the elapsed time of the item, not yet tested.
     *
     * @return The elapsed time is not yet fully tested for Mac.
     */
    @Override
    public long getTime() {
        long time = 0;
        try {
            time = AVPlayerJava.getTime();
            if(time < 0){
                return 0;
            }
        } catch (Exception ignore) {
            LOGGER.log(Level.INFO, "Couldn't get the current time, Mac Video");
        }
        return time;
    }

    /**
     * Get the total time of the item, not yet tested.
     *
     * @return The total time is not yet fully tested for Mac.
     */
    @Override
    public long getTotal() {
        long time = 0;
        try {
            time = AVPlayerJava.getDuration();
            if(time < 0){
                return 0;
            }
        } catch (Exception ignore) {
            LOGGER.log(Level.INFO, "Couldn't get the total time, Mac Video");
        }
        return time;
    }

    /**
     * Get the current volume level of the item, not yet tested.
     *
     * @return The volume level
     */
    @Override
    public int getVolume() {
        int volume = 100;
        try {
            volume = (Double.valueOf(AVPlayerJava.getVolume() * 100)).intValue();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Couldn't get volume level");
        }
        return volume;
    }

    /**
     * TODO: Set the volume level of the item, not yet tested.
     *
     * @param volume Desired volume level
     */
    @Override
    public void setVolume(int volume) {
        try {
            AVPlayerJava.setVolume((double) volume / 100);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Couldn't set volume level");
        }
    }

    /**
     * Currently a convenience method, as everything that should be run on the
     * player thread is funneled through this method, or the runAndWait method.
     * This is so that it will be easier to diagnose threading problems later.
     * <p/>
     * @param r the runnable to run.
     */
    private void runOnVIDThread(Runnable r) {
        r.run();
        /*
         MAC_VID_EXECUTOR.submit(r);
         */

    }

    /**
     * Currently a convenience method, as everything that should be run on the
     * player thread is funneled through this method, or the run method. This is
     * so that it will be easier to diagnose threading problems later.
     *
     * @param r the runnable to run.
     */
    private void runOnVIDThreadAndWait(Runnable r) {
        r.run();
        /*
         try {
         MAC_VID_EXECUTOR.submit(r).get();
         } catch (InterruptedException | ExecutionException ex) {
         LOGGER.log(Level.WARNING, "Interrupted or execution error", ex);
         }
         */

    }

    public void setWindowVisible(boolean visible) {
    }
}
