package org.warifu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.warifu.R

@Composable
fun WarifuLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_warifu_logo),
        contentDescription = "Warifu Logo",
        modifier = modifier.size(size)
    )
}
