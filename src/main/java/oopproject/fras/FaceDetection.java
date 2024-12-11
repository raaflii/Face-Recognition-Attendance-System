package oopproject.fras;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FaceDetection  {
    private final CascadeClassifier faceDetector;
    
    public FaceDetection(String haarCascadePath) {
        faceDetector = new CascadeClassifier(haarCascadePath);

        if (faceDetector.empty()) {
            throw new IllegalArgumentException("Haar Cascade XML tidak valid atau tidak ditemukan: " + haarCascadePath);
        }
    }

    public Rect[] detectFaces(Mat frame) {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(frame, faceDetections);
        return faceDetections.toArray();
    }

    public Mat drawRectangles(Mat frame, Rect[] faces) {
        for (Rect face : faces) {
            Imgproc.rectangle(frame, face.tl(), face.br(), new org.opencv.core.Scalar(0, 255, 0), 2);
        }
        return frame;
    }
}
