package com.github.saturngod;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Decompress {
    private InputStream _zipInputStream;
    private String _location;

    public Decompress(InputStream zipInputStream, String location) {
        _zipInputStream = zipInputStream;
        _location = location;

        _dirChecker("");
    }

    public void unzip() {
        try  {
            ZipInputStream zin = new ZipInputStream(_zipInputStream);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    _dirChecker(ze.getName());
                } else {


                    int size;
                    byte[] buffer = new byte[2048];

                    FileOutputStream fout = new FileOutputStream(_location + File.separatorChar + ze.getName());
                    BufferedOutputStream bufferOut = new BufferedOutputStream(fout, buffer.length);

                    while ((size = zin.read(buffer, 0, buffer.length)) != -1) {
                        bufferOut.write(buffer, 0, size);
                    }




                    bufferOut.flush();
                    bufferOut.close();

                    zin.closeEntry();
                    fout.close();



                }
            }

            zin.close();

        } catch(Exception e) {
            Log.e("Decompress", "unzip", e);
        }

    }

    private void _dirChecker(String dir) {

        try {
            char lastChar = _location.charAt(_location.length() - 1);
            String loc = _location;

            if(lastChar != File.separatorChar)
            {
                loc = loc + File.separator;
            }

            File f = new File(loc + dir);

            if(!f.isDirectory()) {

                f.mkdirs();
            }
        }
        catch(Exception e){
            Log.w("creating file error", e.toString());
        }
    }
}
