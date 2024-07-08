package me.yricky.abcde.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.yricky.abcde.AppState
import me.yricky.abcde.ui.*
import me.yricky.abcde.util.TreeModel
import me.yricky.oh.abcd.AbcBuf
import me.yricky.oh.common.TreeStruct
import me.yricky.oh.abcd.cfm.ClassItem
import me.yricky.oh.abcd.cfm.AbcClass

class AbcOverview(val abc: AbcBuf):Page() {
    override val tag: String = abc.tag

    @Composable
    override fun Page(modifier: Modifier, appState: AppState) {
        AbcOverviewPage(modifier, appState, this)
    }

    private val classMap get()= abc.classes
    var filter by mutableStateOf("")
        private set
    private val treeStruct = TreeModel(TreeStruct(classMap.values, pathOf = { it.name }))
    var classList by mutableStateOf(treeStruct.buildFlattenList())
        private set

    fun isFilterMode() = filter.isNotEmpty()

    var classCount by mutableStateOf(classMap.size)

    fun setNewFilter(str:String){
        filter = str
        if(!isFilterMode()){
            classList = treeStruct.buildFlattenList()
        } else {
            classList = treeStruct.buildFlattenList{ it.pathSeg.contains(filter) }
        }
        classCount = if (isFilterMode()) classList.count { it.second is TreeStruct.LeafNode } else classMap.size
    }

    fun toggleExpand(node: TreeStruct.TreeNode<ClassItem>){
        if(!isFilterMode()){
            treeStruct.toggleExpand(node)
            classCount = classMap.size
            classList = treeStruct.buildFlattenList()
        }
    }

    override fun equals(other: Any?): Boolean {
        if(other !is AbcOverview){
            return false
        }
        return abc == other.abc
    }

    override fun hashCode(): Int {
        return abc.hashCode()
    }
}



@Composable
fun AbcOverviewPage(
    modifier: Modifier,
    appState: AppState,
    abcOverview: AbcOverview
) {

    val scope = rememberCoroutineScope()
    VerticalTabAndContent(modifier, listOf(composeSelectContent{ _: Boolean ->
        Image(Icons.clazz(), null, Modifier.fillMaxSize(), colorFilter = grayColorFilter)
    } to composeContent{
        Column(Modifier.fillMaxSize().padding(end = 4.dp)) {
            var filter by remember(abcOverview.filter) {
                mutableStateOf(abcOverview.filter)
            }
            OutlinedTextField(
                value = filter,
                onValueChange = { _filter ->
                    filter = _filter.replace(" ", "").replace("\n", "")
                    scope.launch {
                        if (abcOverview.classList.isNotEmpty()) {
                            delay(500)
                        }
                        if (_filter == filter) {
                            println("Set:$_filter")
                            abcOverview.setNewFilter(filter)
                        } else {
                            println("drop:${_filter}")
                        }
                    }
                },
                leadingIcon = {
                    Image(Icons.search(), null)
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("${abcOverview.classCount}个类")
                },
            )
            TreeItemList(Modifier.fillMaxWidth().weight(1f), abcOverview.classList,
                onClick = {
                    if (it is TreeStruct.LeafNode) {
                        val clazz = it.value
                        if(clazz is AbcClass){
                            appState.openClass(clazz)
                        }
                    } else if(it is TreeStruct.TreeNode){
                        abcOverview.toggleExpand(it)
                    }
                }) {
                when (val node = it) {
                    is TreeStruct.LeafNode<ClassItem> -> {
                        Image(node.value.icon(), null, modifier = Modifier.padding(end = 2.dp).size(16.dp))
                    }
                    is TreeStruct.TreeNode<ClassItem> -> {
                        Image(Icons.pkg(), null, modifier = Modifier.padding(end = 2.dp).size(16.dp))
                    }
                }
                Text(it.pathSeg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }, composeSelectContent{
        Image(Icons.info(), null, Modifier.fillMaxSize(), colorFilter = grayColorFilter)
    } to composeContent{
        Column {
            Text(abcOverview.abc.tag, style = MaterialTheme.typography.titleLarge)
            Text("文件版本:${abcOverview.abc.header.version}")
            Text("size:${abcOverview.abc.header.fileSize}")
            Text("Class数量:${abcOverview.abc.header.numClasses}")
            Text("行号处理程序数量:${abcOverview.abc.header.numLnps}")
            Text("IndexRegion数量:${abcOverview.abc.header.numIndexRegions}")
        }
    }
    ))
}