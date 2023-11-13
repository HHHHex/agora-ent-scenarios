//
//  srScoreTitleCell.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/5/18.
//

import UIKit

class SRScoreTitleCell: UITableViewCell {

    private var indexLabel: UILabel!
    private var playerLabel: UILabel!
    private var srCountLabel: UILabel!
    private var scoreLabel: UILabel!
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        layoutUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func layoutUI() {
        indexLabel = UILabel()
        indexLabel.text = "排名"
        indexLabel.textAlignment = .center
        indexLabel.textColor = .white
        indexLabel.font = UIFont.systemFont(ofSize: 10)
        self.contentView.addSubview(indexLabel)
        
        playerLabel = UILabel()
        playerLabel.text = "玩家"
        playerLabel.textAlignment = .center
        playerLabel.textColor = .white
        playerLabel.font = UIFont.systemFont(ofSize: 10)
        self.contentView.addSubview(playerLabel)
        
        srCountLabel = UILabel()
        srCountLabel.text = "抢唱成功"
        srCountLabel.textAlignment = .center
        srCountLabel.textColor = .white
        srCountLabel.font = UIFont.systemFont(ofSize: 10)
        self.contentView.addSubview(srCountLabel)
        
        scoreLabel = UILabel()
        scoreLabel.text = "总分"
        scoreLabel.textAlignment = .center
        scoreLabel.textColor = .white
        scoreLabel.font = UIFont.systemFont(ofSize: 10)
        self.contentView.addSubview(scoreLabel)
        
        self.backgroundColor = .clear
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        indexLabel.frame = CGRect(x: 0, y: 0, width: self.bounds.size.width / 5.0, height: 28)
        playerLabel.frame = CGRect(x: indexLabel.frame.maxX, y: 0, width: self.bounds.size.width / 5.0 * 2, height: 28)
        srCountLabel.frame = CGRect(x: playerLabel.frame.maxX, y: 0, width: self.bounds.size.width / 5.0, height: 28)
        scoreLabel.frame = CGRect(x: srCountLabel.frame.maxX, y: 0, width: self.bounds.size.width / 5.0, height: 28)
    }
    
    
}