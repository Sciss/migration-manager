package ssol.tools.mima

import scala.reflect.NameTransformer

object MemberInfo {

 /** The index of the string $_setter_$ in this string */
  private def setterIdx(name: String) = name.indexOf(setterTag)
  
  private val setterTag = "$_setter_$"
  private val setterSuffix = "_$eq"

  def maybeSetter(name: String) = name.endsWith(setterSuffix)
}

class MemberInfo(val owner: ClassInfo, val name: String, val flags: Int, val sig: String) {
  override def toString = "def "+name+": "+sig

  def decodedName = NameTransformer.decode(name) 

  def fieldString = "field "+decodedName+" in "+owner.classString
  def methodString = "method "+decodedName+Type.fromSig(sig)+" in "+owner.classString

  def tpe: Type = Type.fromSig(sig)

  def staticImpl = owner.implClass.staticImpl(this)

  def isMethod: Boolean = sig(0) == '('

  def parametersSig = {
    assert(isMethod)
    sig substring (1, sig indexOf ")")
  }

  def matchesType(other: MemberInfo): Boolean = 
    if (isMethod) other.isMethod && parametersSig == other.parametersSig
    else !other.isMethod && sig == other.sig

  def resultSig = {
    assert(sig(0) == '(')
    sig substring ((sig indexOf ")") + 1)
  }
  var codeOpt: Option[(Int, Int)] = None

  def isClassConstructor = name == "<init>"

  def needCode = isClassConstructor

  import MemberInfo._

  var isTraitSetter = maybeSetter(name) && setterIdx(name) >= 0

  def isPublic: Boolean = ClassfileParser.isPublic(flags) 

  def isDeferred: Boolean = ClassfileParser.isDeferred(flags) 

  def hasSyntheticName: Boolean = decodedName contains '$'
  
  def isAccessible: Boolean = isPublic && !hasSyntheticName

  /** The name of the getter corresponding to this setter */
  private def getterName: String = {
    val sidx = setterIdx(name)
    val start = if (sidx >= 0) sidx + setterTag.length else 0
    name.substring(start, name.length - setterSuffix.length)
  }

  /** The getter that corresponds to this setter */
  def getter: MemberInfo = {
    val argsig = "()" + parametersSig
    owner.methods.get(getterName) find (_.sig == argsig) get
  }

  def description: String = name+": "+sig+" from "+owner.description
}
