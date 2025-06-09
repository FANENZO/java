public class hit {
    if(currentBackgroundIndex==1)
    {
            if (playerX + playerWidth > block23X && playerX < block23X + blockWidth &&
                    playerY + playerHeight > blockY && playerY < blockY + blockHeight) {

                // 頂部碰撞
                if (playerY + playerHeight > blockY && playerY + playerHeight - jumpSpeed <= blockY) {
                    playerY = blockY - playerHeight; // 停在磚塊上方
                    isFalling = false;
                    jump = false;
                }

                // 底部碰撞
                else if (playerY < blockY + blockHeight && playerY + jumpSpeed >= blockY + blockHeight) {
                    isJumping = false;
                    isFalling = true; // 開始下落
                }

                // 左側碰撞
                else if (playerX + playerWidth > block23X && playerX < block23X + blockWidth / 2) {
                    playerX = block23X - playerWidth; // 阻止角色穿過磚塊左側
                }

                // 右側碰撞
                else if (playerX < block23X + blockWidth && playerX + playerWidth > block22X + blockWidth / 2) {
                    playerX = block23X + blockWidth; // 阻止角色穿過磚塊右側
                }
            }
        }
    }

