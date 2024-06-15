package me.yricky.oh.abcd.isa

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import me.yricky.oh.abcd.code.Code
import me.yricky.oh.abcd.isa.bean.Isa

class Asm(
    val code: Code,
) {
    companion object{
        val yaml = YAMLMapper()
        val asmMap = AsmMap(yaml.readValue(Asm::class.java.classLoader.getResourceAsStream("abcde/isa.yaml"),object : TypeReference<Isa>(){}))
    }
    val list:List<AsmItem> by lazy{
        val li = ArrayList<AsmItem>()
        var off = 0
        while (off < code.codeSize){
            val initOff = off
            val opCode = code.instructions.get(off)
            off += 1
            asmMap.prefixInstMap[opCode]?.let {
                val subOpCode = code.instructions.get(off)
                off += 1
                val inst = it[subOpCode] ?: throw IllegalStateException("No this subOpCode:${subOpCode.toString(16)} in opCode:${opCode.toString(16)}")
                off += inst.argSize()
                li.add(AsmItem(inst,initOff))
            } ?: asmMap.insMap[opCode]?.let { ins ->
                off += ins.argSize()
                li.add(AsmItem(ins, initOff))
            } ?: throw IllegalStateException("No this opCode:${opCode.toString(16)},off:${off - 1}")
        }
        li
    }

    fun asmString(item: AsmItem):String{
        val sb = StringBuilder()
        val initOff = item.codeOffset
        var off = item.codeOffset //opCode之后的

        with(code){
            val oprand = mutableListOf<Number>()
            val iter = item.ins.format.iterator()
            while (iter.hasNext()){
                val thisFmt = iter.next()
                if(thisFmt is Inst.InstFmt.Prefix || thisFmt is Inst.InstFmt.OpCode){
                    off += 1
                } else if(thisFmt.bitSize == 4){
                    val nextFmt = iter.next()
                    if(nextFmt.bitSize != 4){
                        throw IllegalStateException()
                    }
                    val value = instructions.get(off).toUByte().toInt()
                    oprand.add(value.shr(4))
                    oprand.add(value and  0xf)
                    off += 1
                } else if(thisFmt.bitSize == 8){
                    val value = instructions.get(off)
                    oprand.add(value)
                    off += 1
                } else if(thisFmt.bitSize == 16){
                    val value = instructions.getShort(off).toUShort()
                    oprand.add(value.toInt())
                    off += 2
                } else if(thisFmt.bitSize == 32){
                    val value = instructions.getInt(off)
                    oprand.add(value)
                    off += 4
                } else if(thisFmt.bitSize == 64){
                    val value = instructions.getLong(off)
                    oprand.add(value)
                    off += 8
                } else throw IllegalStateException()
            }
            sb.append(item.ins.asmString(code,oprand))
        }
        sb.append(" ".repeat((8 - sb.length%8)))
        sb.append("//")
        (initOff until off).forEach {
            sb.append(String.format("%02X",code.instructions.get(it)))
        }
        return sb.toString()
    }

    class AsmItem(
        val ins:Inst,
        val codeOffset:Int
    )
}