package client;

import javax.sound.sampled.*;

public class SoundPlayer {

    // millis 만큼 재생되는 사운드 플레이어
    public static void playForMillis(String path, Object caller, int millis) {
        new Thread(() -> {
            try {
                var res = caller.getClass().getResource(path);
                if (res == null) {
                    System.out.println("사운드 파일 없음: " + path);
                    return;
                }

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(res);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);

                clip.start();
                Thread.sleep(millis);  // 원하는 시간만큼 재생
                clip.stop();
                clip.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
