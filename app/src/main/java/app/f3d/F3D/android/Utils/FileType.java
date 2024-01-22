package app.f3d.F3D.android.Utils;

import java.util.Arrays;

public class FileType {

    public final static String[] supportedMimeTypes = {
            "image/tiff",                    // .tif,
            "x-world/x-vrml",                // .wrl,
            "application/dicom",             // .dcm,
            "application/octet-stream",      // .3ds, .glb, .mhd, .nrrd, .obj, .ply, .pts, .vtk, .vtu, .vtp, .vti, .vtr, .vts, .vtm
            "application/vnd.ms-pki.stl",    // .stl,
    };
    public static boolean checkFileTypeMethod(String fileType){

        String[] fileTypesUsingGeometry = {
                "obj",
                "ply",
                "pts",
                "stl",
                "vtk",
                "vtu",
                "vtp",
                "vti",
                "vtr",
                "vts",
                "dcm",      // -> Doubts and needs Volume rendering
                "mhd",      // -> Doubts and needs Volume rendering
                "nrd",      // -> Doubts and needs Volume rendering
                "tif",      // -> Doubts and needs Volume rendering
                "vtm"       // -> Doubts and needs Volume rendering
        };

        return Arrays.asList(fileTypesUsingGeometry).contains(fileType);
    }
}
