package simpleservice

import scala.language.experimental.macros
import scala.reflect.macros.Context
import SimpleService._

object SimpleObjectBuilder {
  def apply[T] : SimpleObjectBuilder[T] = macro Macros.generateObjectBuilder[T]
}

case class Ignore extends scala.annotation.StaticAnnotation
case class ObjectPath(path: String) extends scala.annotation.StaticAnnotation
case class ObjectType(objType: String) extends scala.annotation.StaticAnnotation

object Macros {
  def toDebugString(obj: AnyRef) = s"'${obj}': ${obj.getClass}"



  def generateObjectBuilder[T: c.WeakTypeTag](c: Context) : c.Expr[SimpleObjectBuilder[T]] = {
    import c.universe._

    // Get a reference to the constructor of the type.
    val typeConstructor = weakTypeOf[T]

    val constructor = typeConstructor.declarations.collect({
      case MethodSymbolTag(method) if method.isPrimaryConstructor => method
    }).head

    // Get the parameter symbols of the constructor.
    // Filter out any with the @Ignore annotation.
    val parameters = constructor.paramss.head.filter(p =>
      p.annotations.find(_.tpe =:= typeOf[Ignore]).isEmpty
    )

    /**
     * Generates:
     *  service.setBusinessObjectValue(sessionId, instanceId, "paramName", instance.paramName)
     */
    def generateValueSetter(parameter: Symbol) = Apply(
      Select(Ident(newTermName("service")), newTermName("setBusinessObjectValue")),
      List(
        Ident(newTermName("sessionId")),
        Ident(newTermName("instanceId")),
        Literal(Constant(parameter.name.toTermName.toString.trim)) : Tree,
        Select(
          Ident(newTermName("instance")),
          newTermName(parameter.name.toTermName.toString.trim)
        ) : Tree
      )
    )

    /**
     * Generates:
     *  service.setBusinessObjectValue(sessionId, instanceId, "paramName", instance.paramName.mkString("|"))
     */
    def generateSequenceSetter(parameter: Symbol) = Apply(
      Select(Ident(newTermName("service")), newTermName("setBusinessObjectValue")),
      List(
        Ident(newTermName("sessionId")),
        Ident(newTermName("instanceId")),
        Literal(Constant(parameter.name.toTermName.toString.trim)) : Tree,
        Apply(Select(
          Select(
            Ident(newTermName("instance")),
            newTermName(parameter.name.toTermName.toString.trim)),
            newTermName("mkString")
        ), List(Literal(Constant("|")))) : Tree
      )
    )

    /**
     * Generates:
     *   SimpleObjectBuilder[T](instance.paramName)(service, sessionId, path)
     */
    def generateObjectSetter(parameter: Symbol, path: Tree) =
      Apply(Apply(TypeApply(
        Select(
          Ident(newTermName("SimpleObjectBuilder")), newTermName("apply")
        ), List(Ident(parameter.typeSignature.typeSymbol)) // needs to be the type of the parameter.
      ), List(
        Select(Ident(newTermName("instance")), newTermName(parameter.name.toTermName.toString.trim))
      )),
      List(
        Ident(newTermName("service")),
        Ident(newTermName("sessionId")),
        path
      )
    )

    // List of types we can directly serialise
    val acceptableTypes = List(
      typeOf[Int],
      typeOf[String],
      typeOf[Long]
    )

    def isAcceptableType(pType: Type) =
      acceptableTypes.foldRight(false)((typ, memo) => memo || pType <:< typ)

    val appliedParameters = parameters.map(p => {
      if (p.typeSignature <:< typeOf[Seq[_]]) {
        generateSequenceSetter(p)
      } else if (isAcceptableType(p.typeSignature)) {
        generateValueSetter(p)
      } else {
        val pathAnnotation = p.annotations.find(_.tpe =:= typeOf[ObjectPath])
        pathAnnotation match {
          case Some(pathAnnotation) =>
            generateObjectSetter(p, pathAnnotation.scalaArgs.head)
          case None => {
            c.abort(c.enclosingPosition, s"Complex object in field `${p.name}` on type `${typeConstructor}` does not have @ObjectPath annotation. Use @Ignore if you don't want it serialized.")
          }
        }
      }
    })

    val createObject = reify {
      (instance: T) =>
      (service: SimpleService, sessionId: SessionId, path: Path) => {
        val instanceId = service.createBusinessObject(sessionId, "objType", path)

        c.Expr[Unit](Block(appliedParameters.toSeq: _*)).splice

        service.getPath(sessionId, instanceId)
      }
    }

    println(createObject)

    createObject
  }
}