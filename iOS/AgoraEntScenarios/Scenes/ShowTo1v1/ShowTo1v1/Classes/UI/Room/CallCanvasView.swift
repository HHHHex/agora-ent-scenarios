//
//  CallCanvasView.swift
//  Pure1v1
//
//  Created by wushengtao on 2023/8/4.
//

import Foundation

class CallCanvasView: UIView {
    lazy var canvasView = UIView()
    
    var tapClosure: (()->())?
    
    lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.systemFont(ofSize: 12)
        label.textColor = .white
        return label
    }()
    
    
    private lazy var leaveIconView: UIImageView = {
        let imageView = UIImageView(image: UIImage.sceneImage(name: "icon_user_leave")!)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var leaveTipsLabel = {
        let label = UILabel()
        label.textColor = .white
        label.font = .systemFont(ofSize: 13)
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = .center
        paragraphStyle.lineHeightMultiple = 1.3
        label.attributedText = NSMutableAttributedString(string: "user_list_user_leave".showTo1v1Localization(),
                                                        attributes: [.paragraphStyle: paragraphStyle])
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(leaveIconView)
        addSubview(leaveTipsLabel)
        addSubview(canvasView)
        addSubview(titleLabel)
        
        let tapGes = UITapGestureRecognizer {[weak self] _ in
            self?.tapClosure?()
        }
        addGestureRecognizer(tapGes)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        canvasView.frame = bounds
        titleLabel.sizeToFit()
        titleLabel.aui_bl = CGPoint(x: 11, y: aui_height - 10)
        
        //add shadow
        titleLabel.layer.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.5).cgColor
        titleLabel.layer.shadowOpacity = 1
        titleLabel.layer.shadowRadius = 4
        titleLabel.layer.shadowOffset = CGSize(width: 0, height: 1)
        
        leaveIconView.sizeToFit()
        if let oldWidthConstraint = leaveTipsLabel.constraints.first(where: { $0.firstAttribute == .width }) {
            leaveTipsLabel.removeConstraint(oldWidthConstraint)
        }
        leaveTipsLabel.widthAnchor.constraint(equalToConstant: aui_width).isActive = true
        leaveTipsLabel.sizeToFit()
        leaveIconView.aui_center = CGPoint(x: aui_width / 2, y: aui_height / 2 - leaveTipsLabel.aui_height / 2)
        leaveTipsLabel.aui_centerX = leaveIconView.aui_centerX
        leaveTipsLabel.aui_top = leaveIconView.aui_bottom
    }
}
