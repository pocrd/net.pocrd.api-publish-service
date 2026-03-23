-- test_user 表初始化数据
INSERT INTO `test_user` (`username`, `email`, `phone`, `status`) VALUES
('user001', 'user001@example.com', '13800138001', 1),
('user002', 'user002@example.com', '13800138002', 1),
('user003', 'user003@example.com', '13800138003', 1),
('user004', 'user004@example.com', '13800138004', 1),
('user005', 'user005@example.com', '13800138005', 1),
('user006', 'user006@example.com', '13800138006', 0),
('user007', 'user007@example.com', '13800138007', 1),
('user008', 'user008@example.com', '13800138008', 1),
('user009', 'user009@example.com', '13800138009', 0),
('user010', 'user010@example.com', '13800138010', 1);

-- test_product 表初始化数据
INSERT INTO `test_product` (`product_code`, `product_name`, `price`, `stock`, `category`) VALUES
('P001', 'iPhone 15 Pro', 7999.00, 100, '手机'),
('P002', 'MacBook Pro 14', 14999.00, 50, '电脑'),
('P003', 'AirPods Pro 2', 1899.00, 200, '耳机'),
('P004', 'iPad Air 5', 4799.00, 80, '平板'),
('P005', 'Apple Watch S9', 2999.00, 120, '手表'),
('P006', '小米14', 3999.00, 150, '手机'),
('P007', '华为Mate 60', 5999.00, 80, '手机'),
('P008', '索尼WH-1000XM5', 2499.00, 60, '耳机'),
('P009', '戴尔XPS 13', 8999.00, 40, '电脑'),
('P010', '任天堂Switch', 2099.00, 90, '游戏');

-- test_order 表初始化数据
INSERT INTO `test_order` (`order_no`, `user_id`, `amount`, `status`, `remark`) VALUES
('ORD202403050001', 1, 7999.00, 1, 'iPhone订单'),
('ORD202403050002', 2, 14999.00, 1, 'MacBook订单'),
('ORD202403050003', 1, 1899.00, 0, 'AirPods订单'),
('ORD202403050004', 3, 4799.00, 1, 'iPad订单'),
('ORD202403050005', 4, 2999.00, 2, 'Watch订单-已取消'),
('ORD202403050006', 5, 3999.00, 0, '小米14订单'),
('ORD202403050007', 2, 5999.00, 1, '华为订单'),
('ORD202403050008', 6, 2499.00, 1, '索尼耳机订单'),
('ORD202403050009', 7, 8999.00, 0, '戴尔电脑订单'),
('ORD202403050010', 8, 2099.00, 1, 'Switch订单');
