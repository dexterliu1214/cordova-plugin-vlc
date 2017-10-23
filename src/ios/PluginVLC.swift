import UIKit
import AVFoundation

class UIScanViewController: UIViewController{
    public var plugin:PluginVLC? = nil
    public var callback:String? = nil
    let captureDevice:AVCaptureDevice = AVCaptureDevice.defaultDevice(withMediaType:AVMediaTypeVideo)
    lazy var input:AVCaptureDeviceInput = try! AVCaptureDeviceInput.init(device: self.captureDevice)
    var captureSession:AVCaptureSession = AVCaptureSession()
    lazy var videoPreviewLayer:AVCaptureVideoPreviewLayer = AVCaptureVideoPreviewLayer(session: self.captureSession)
    // var captureOutput:AVCapturePhotoOutput = AVCapturePhotoOutput()
    var button:UIButton = UIButton(frame: CGRect(x: 100, y: 100, width: 100, height: 44))

    var captureOutput:AVCaptureStillImageOutput = AVCaptureStillImageOutput()
    lazy var connection:AVCaptureConnection = self.captureOutput.connection(withMediaType: AVMediaTypeVideo)
    var running = true
    override func viewDidLoad() {
        super.viewDidLoad()
        running = true
        let authorizationStatus = AVCaptureDevice.authorizationStatus(forMediaType:AVMediaTypeVideo)

		switch authorizationStatus {
            case .notDetermined:
                AVCaptureDevice.requestAccess(forMediaType:AVMediaTypeVideo,
                    completionHandler: { (granted:Bool) -> Void in
                        if granted {
                            self.initialize()
                        }
                        else {
                            self.showAccessDeniedMessage()
                        }
                })
            case .authorized:
                self.initialize()
            case .denied, .restricted:
                self.showAccessDeniedMessage()
		}
    }

    func showAccessDeniedMessage(){
        self.close()
    }

    func initialize(){
        setupPreviewLayer()
        setupCloseButton()
//        DispatchQueue.global().async {
            self.configCaptureSession()
            self.captureSession.startRunning()
            self.takePhoto()
//        }
    }

    func configCaptureSession(){
        captureSession.beginConfiguration()
        if captureSession.canAddInput(input) {
            captureSession.addInput(input)
        }
        if captureSession.canAddOutput(captureOutput) {
            captureSession.addOutput(captureOutput)
        }
        captureSession.commitConfiguration()
    }

    func setupPreviewLayer(){
        videoPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        videoPreviewLayer.frame = view.layer.bounds
        view.layer.addSublayer(videoPreviewLayer)
    }

    func setupCloseButton(){
        button.setTitle("關閉", for: .normal)
        button.addTarget(self, action: #selector(UIScanViewController.close), for: .touchUpInside)
        view.addSubview(button)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -50).isActive = true
        button.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
    }

    func close() {
        running = false
        videoPreviewLayer.removeFromSuperlayer()
        self.plugin = nil
        self.callback = nil
        //        self.captureSession.stopRunning()
        DispatchQueue.main.async {
            self.dismiss(animated: true, completion: {
//                self.captureSession.stopRunning()
                print("close")
            })
        }

    }

    func genExposureSettings() -> [AVCaptureBracketedStillImageSettings] {
        let duration = self.captureDevice.activeFormat.minExposureDuration
        let iso = self.captureDevice.activeFormat.maxISO
        let exposureSettings = AVCaptureManualExposureBracketedStillImageSettings.manualExposureSettings(withExposureDuration: duration, iso: iso)!
        return [exposureSettings]
    }

    // func genSettings() -> AVCapturePhotoBracketSettings? {
    //     return AVCapturePhotoBracketSettings(rawPixelFormatType: 0, processedFormat: nil, bracketedSettings: self.genExposureSettings())
    // }

    // func takePhoto(){
    //         guard let settings = self.genSettings() else {
    //             print("no setting")
    //             return
    //         }
    //         self.captureOutput.capturePhoto(with: settings, delegate: self)
    // }

    func takePhoto()
    {
        if !self.running {
            return
        }
        captureOutput.captureStillImageBracketAsynchronously(from: self.connection, withSettingsArray: self.genExposureSettings(), completionHandler: { (buffer:CMSampleBuffer?, settings:AVCaptureBracketedStillImageSettings?, error:Error?) in
            if let error = error {
                print(error)
                return
            }

            guard let buf = buffer else {
                print("no buffer")
                return
            }

            guard let img = UIImage.init(data: AVCaptureStillImageOutput.jpegStillImageNSDataRepresentation(buf)) else {
                print("no image")
                return
            }

            guard let res = itriVLClib.startPixel(img, round: 0, known: false, length:8) as? [String] else {
                print("no res")
                return
            }

            if res.count > 3 && res[0] == res[2] {
                self.parse(res)
                return
            }

            if !self.running {
                return
            }
            self.takePhoto()
        })
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        get { return UIInterfaceOrientationMask.portrait }
    }

    func parse(_ res:[String]) {
        guard let plugin = self.plugin, let callback = self.callback else {
            print("no plugin")
            return
        }
        if res.count > 3 && res[0] == res[2] && res[0] != "00000000" && res[0] != "11111111" {
            let code = res[0]

//            DispatchQueue.main.async {
                plugin.returnSuccess(text:code, callback:callback)
//            }
        }
    }
}

// extension UIScanViewController : AVCapturePhotoCaptureDelegate {

//     func capture(_ captureOutput: AVCapturePhotoOutput, didFinishProcessingPhotoSampleBuffer photoSampleBuffer: CMSampleBuffer?, previewPhotoSampleBuffer: CMSampleBuffer?, resolvedSettings: AVCaptureResolvedPhotoSettings, bracketSettings: AVCaptureBracketedStillImageSettings?, error: Error?) {        print("capture")
//         if !self.captureSession.isRunning {
//             return
//         }

//        guard let photoSampleBuffer = photoSampleBuffer else {
//            print("no photo buffer")
//            return
//        }

//        guard let photoData = AVCapturePhotoOutput.jpegPhotoDataRepresentation(forJPEGSampleBuffer: photoSampleBuffer, previewPhotoSampleBuffer: nil) else {
//            print("no data")
//            return
//        }
//        //
//        guard let img = UIImage.init(data: photoData) else {
//            print("no image")
//            return
//        }

//        guard let res = itriVLClib.startPixel(img, round: 0, known: false) as? [String] else {
//            print("no res")
//            return
//        }

//         self.parse(res)

//         if self.captureSession.isRunning {
//             self.takePhoto()
//         }
//     }
// }

@objc(PluginVLC) class PluginVLC : CDVPlugin {
    var scanController:UIScanViewController? = nil
    @objc(scan:)
    func scan(command: CDVInvokedUrlCommand) {
        scanController = UIScanViewController()
        scanController?.plugin = self
        scanController?.callback = command.callbackId
        self.viewController?.present(
            scanController!,
            animated: true,
            completion: nil
        )
    }

    func returnSuccess(text:String, callback:String) {
        let pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: text
        )

        self.commandDelegate!.send(
            pluginResult,
            callbackId: callback
        )
        scanController?.close()
        scanController = nil
    }
}