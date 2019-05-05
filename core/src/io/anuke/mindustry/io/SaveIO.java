package io.anuke.mindustry.io;

import io.anuke.arc.collection.*;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.io.CounterInputStream;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.versions.Save1;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.*;

public class SaveIO{
    /** Format header. This is the string 'MSAV' in ASCII. */
    public static final byte[] header = {77, 83, 65, 86};
    public static final IntMap<SaveFileVersion> versions = new IntMap<>();
    public static final Array<SaveFileVersion> versionArray = Array.with(new Save1());

    static{
        for(SaveFileVersion version : versionArray){
            versions.put(version.version, version);
        }
    }

    public static SaveFileVersion getSaveWriter(){
        return versionArray.peek();
    }

    public static void saveToSlot(int slot){
        FileHandle file = fileFor(slot);
        boolean exists = file.exists();
        if(exists) file.moveTo(backupFileFor(file));
        try{
            write(fileFor(slot));
        }catch(Exception e){
            if(exists) backupFileFor(file).moveTo(file);
            throw new RuntimeException(e);
        }
    }

    public static void loadFromSlot(int slot) throws SaveException{
        load(fileFor(slot));
    }

    public static DataInputStream getSlotStream(int slot){
        return new DataInputStream(new InflaterInputStream(fileFor(slot).read(bufferSize)));
    }

    public static DataInputStream getBackupSlotStream(int slot){
        return new DataInputStream(new InflaterInputStream(backupFileFor(fileFor(slot)).read(bufferSize)));
    }

    public static boolean isSaveValid(int slot){
        return isSaveValid(getSlotStream(slot)) || isSaveValid(getBackupSlotStream(slot));
    }

    public static boolean isSaveValid(FileHandle file){
        return isSaveValid(new DataInputStream(new InflaterInputStream(file.read(bufferSize))));
    }

    public static boolean isSaveValid(DataInputStream stream){
        try{
            getData(stream);
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static SaveMeta getData(int slot){
        return getData(getSlotStream(slot));
    }

    public static SaveMeta getData(DataInputStream stream){

        try{
            int version = stream.readInt();
            SaveMeta meta = versions.get(version).getData(stream);
            stream.close();
            return meta;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static FileHandle fileFor(int slot){
        return saveDirectory.child(slot + "." + Vars.saveExtension);
    }

    public static FileHandle backupFileFor(FileHandle file){
        return file.sibling(file.name() + "-backup." + file.extension());
    }

    public static void write(FileHandle file){
        write(new DeflaterOutputStream(file.write(false, bufferSize)){
            byte[] tmp = {0};

            public void write(int var1) throws IOException{
                tmp[0] = (byte)(var1 & 255);
                this.write(tmp, 0, 1);
            }
        });
    }

    public static void write(OutputStream os){
        DataOutputStream stream;

        try{
            stream = new DataOutputStream(os);
            getVersion().write(stream);
            stream.close();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void load(FileHandle file) throws SaveException{
        try{
            //try and load; if any exception at all occurs
            load(new InflaterInputStream(file.read(bufferSize)));
        }catch(SaveException e){
            e.printStackTrace();
            FileHandle backup = file.sibling(file.name() + "-backup." + file.extension());
            if(backup.exists()){
                load(new InflaterInputStream(backup.read(bufferSize)));
            }else{
                throw new SaveException(e.getCause());
            }
        }
    }

    public static void load(InputStream is) throws SaveException{
        try(CounterInputStream counter = new CounterInputStream(is); DataInputStream stream = new DataInputStream(counter)){
            logic.reset();
            int version = stream.readInt();
            SaveFileVersion ver = versions.get(version);

            ver.read(stream, counter);
        }catch(Exception e){
            throw new SaveException(e);
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public static SaveFileVersion getVersion(){
        return versionArray.peek();
    }

    public static class SaveException extends RuntimeException{
        public SaveException(Throwable throwable){
            super(throwable);
        }
    }
}
