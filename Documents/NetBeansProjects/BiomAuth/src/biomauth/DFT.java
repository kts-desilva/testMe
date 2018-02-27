/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package biomauth;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 *
 * @author User
 */
public class DFT {

    double real, img;

    public DFT() {
        this.real = 0.0;
        this.img = 0.0;
    }

    public static void recordSound(final String fname) {
        final AudioFormat audioFormat = new AudioFormat(16000, 8, 2, true, true);
        final DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line not supported");
        }
        try {
            final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open();
            System.out.println("recording starts");
            line.start();

            Thread stopping = new Thread(new Runnable() {
                @Override
                public void run() {
                    AudioInputStream st = new AudioInputStream(line);
                    File wf = new File(fname);
                    System.out.println(st.getFrameLength());
                    try {
                        AudioSystem.write(st, AudioFileFormat.Type.WAVE, wf);
                    } catch (IOException ex) {
                        
                    }
                }
            });

            stopping.start();
            Thread.sleep(1500);
            line.stop();
            line.close();
            System.out.println("Ended");

        } catch (InterruptedException ex) {
            
        } catch (LineUnavailableException ex) {
            
        }
    }

    public static double[] getData(String fname) throws IOException, WavFileException {

        // Open the wav file specified as the first argument
        WavFile wavFile = WavFile.openWavFile(new File(fname));

        // Display information about the wav file
        wavFile.display();

        // Get the number of audio channels in the wav file
        int numChannels = wavFile.getNumChannels();
        int maxDataPoints = 100 * numChannels * 30000;

        double[] maxs = new double[300];

        System.out.println("MD: " + maxDataPoints);

        // Create a buffer of 100 frames
        double[] buffer = new double[100 * numChannels];
        double ar[] = new double[maxDataPoints];

        int framesRead;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int p = 0;
        double[] newb = new double[100 * numChannels];

        double[] maxBuf = new double[100 * numChannels];

        int ti = 0;
        do {
            // Read frames into buffer
            framesRead = wavFile.readFrames(buffer, 100);
            boolean same = true;

            ti++;
            // Loop through frames and look for minimum and maximum value
            for (int s = 0; s < framesRead * numChannels; s++) {
                //System.out.println(buffer[s]);
                if (p < 100 * numChannels * 40000) {
                    ar[p] = buffer[s];
                }
                if (same && s < framesRead * numChannels - 1) {
                    if ((buffer[s] - buffer[s + 1]) * 100 < 100) {
                        same = true;
                    } else {
                        same = false;
                    }
                }
                p++;
                if (buffer[s] > max) {
                    max = buffer[s];
                    System.arraycopy(buffer, 0, maxBuf, 0, buffer.length);
                }
                if (buffer[s] < min) {
                    min = buffer[s];
                }
            }
            if (same) {
                System.arraycopy(buffer, 0, newb, 0, buffer.length);
            }
            maxs[ti] = max;
            //smooth(buffer);
        } while (framesRead != 0);


        System.out.println("Max : " + max);
        System.out.println("Las ti : " + ti);

        //doDFT(maxs);

        // Close the wavFile
        wavFile.close();
        double avg = 0;
        for (int i = 0; i < newb.length; i++) {
            avg += newb[i] * 100;
        }
        System.out.println("avg : " + avg / newb.length);

        return doDFT(maxBuf);


    }

    public static double[] doDFT(double[] ar) {
        int k = ar.length * 2;
        System.out.println("Buffer len : " + ar.length);

        DFT[] dft_val = new DFT[k];
        double[] dft_vals = new double[k];

        System.out.println("The coefficients are: ");
        for (int j = 0; j < k; j++) {
            dft_val[j] = new DFT();
            for (int i = 0; i < ar.length; i++) {
                dft_val[j].real += ar[i] * Math.cos((2 * i * j * Math.PI) / ar.length);;
                dft_val[j].img += ar[i] * Math.sin((2 * i * j * Math.PI) / ar.length);;
                dft_vals[j] += ar[i] * Math.cos((2 * i * j * Math.PI) / ar.length);
            }
            //System.out.println("(" + dft_val[j].real + ") - " + "(" + dft_val[j].img + " i)");
        }

        double md = dft_vals[0];
        int tt = 0;

        double[] data = new double[2];

        for (int i = 1; i < dft_vals.length; i++) {
            if (Math.abs(dft_vals[i]) > md) {
                md = Math.abs(dft_vals[i]);
                tt = i;
            }
        }
        System.out.println("MS : " + md);
        System.out.println("MSI : " + tt);

        data[0] = md;
        data[1] = tt;

        return data;

    }

    public static void writeData(int uid, double ms, double msi) throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/User_Info", "senuri", "123");
        PreparedStatement prepareStatement = con.prepareStatement("Insert into VoiceFre values(?,?,?) ");
        prepareStatement.setDouble(1, uid);
        prepareStatement.setDouble(2, ms);
        prepareStatement.setDouble(3, msi);
        int executeUpdate = prepareStatement.executeUpdate();
    }

    public static int classifyData(double ms, double msi) throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/User_Info", "senuri", "123");
        PreparedStatement prepareStatement = con.prepareStatement("select * from VoiceFre");
        ResultSet rst = prepareStatement.executeQuery();
        //ArrayList <Double> sd=new ArrayList<>();
        double tot = ms + msi;
        HashMap<Integer, Double> sd = new HashMap<>();
        while (rst.next()) {
            
            int uid = rst.getInt(1);
            double dms = rst.getDouble(2);
            double dmsi = rst.getDouble(3);
            double totr = dms + dmsi;
            System.out.println("Tot : "+tot);
            System.out.println("Totr : "+totr);
            sd.put(uid, Math.abs(tot - totr));
        }
        double min = Double.MAX_VALUE;
        int id = 0;
        Set<Integer> keySet = sd.keySet();
        for (Integer vd : keySet) {
            System.out.println("Diff : "+sd.get(vd));
            if (min > sd.get(vd)) {
                min = sd.get(vd);
                id = vd;
            }
        }
        return id;
    }

    public static void main(String args[]) {

        try {
            /*Scanner scanner=new Scanner(System.in);
            System.out.print("Input UID : ");
            int uid=scanner.nextInt();
            
            recordSound("testau34.wav");
            Thread.sleep(3000);
            double[] data = getData("testau34.wav");
            writeData(uid, data[0], data[1]);*/
            
            recordSound("testau35.wav");
            
            Thread.sleep(3000);
            double[] data1 = getData("testau35.wav");
            int id=classifyData(data1[0], data1[1]);
            System.out.println("Class : " + id);
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DFT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(DFT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(DFT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DFT.class.getName()).log(Level.SEVERE, null, ex);
        } catch (WavFileException ex) {
            Logger.getLogger(DFT.class.getName()).log(Level.SEVERE, null, ex);
        }


    }
}
