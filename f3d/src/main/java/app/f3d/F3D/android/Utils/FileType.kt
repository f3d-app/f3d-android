package app.f3d.F3D.android.Utils

object FileType {
    val supportedMimeTypes: Array<String> = arrayOf<String>(
        "image/tiff",  // .tif,
        "x-world/x-vrml",  // .wrl,
        "application/dicom",  // .dcm,
        "application/octet-stream",  // .3ds, .glb, .mhd, .nrrd, .obj, .ply, .pts, .vtk, .vtu, .vtp, .vti, .vtr, .vts, .vtm
        "application/vnd.ms-pki.stl",  // .stl,
    )
}
