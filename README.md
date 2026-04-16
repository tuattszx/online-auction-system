- Trước khi code, thực hiện lệnh sau:
    - git checkout main  (ý nghĩa là quay trở về main để mình pull code)
    - git pull    (lấy code của mọi người trước khi code, tránh xung đột conflict)

- Sau khi code, đẩy lên nhánh của bản thân:
    # đẩy code lên nhánh bản thân
    - git add .
    - git commit -m "....."
    - git push origin "tên branch của bản thân"     (không có dấu nháy kép nhé. ví dụ branch của mình là backend thì git push origin backend)
    
    # pull code 1 lần nữa cho đỡ
    - git checkout main
    - git pull
    # thấy code ổn thì thực hiện dòng dưới này
    # gộp nhánh mình vào main
    - git merge "tên nhánh bản thân"

    # đẩy lên main
    - git push origin main
    
    # quay về branch bản thân
    - git checkout "tên branch bản thân"