import java.util.Date
import org.specs2.Specification
import simpleservice.{ObjectType, ObjectPath, Ignore, SimpleObjectBuilder}

@ObjectType("nickname")
case class Nickname(
  name: String
)

@ObjectType("name")
case class Name(
  firstName: String,
  familyName: String,
  otherNames: Seq[String],
  preferredName: String,
  @Ignore twitterHandle: String,
  @ObjectPath("/nickname") nickname: Nickname
)

class SimpleServiceSpec extends Specification { def is = s2"""
Test Function (helps us write the macro)                      ${debugFunction}
"""

  def debugFunction = {
    val builder = SimpleObjectBuilder[Name]

    builder(Name(
      "Laurence",
      "Rouesnel",
      List(),
      "Laurence",
      "@LRouesnel",
      Nickname("Macro geek")
    ))

    ok
  }

}
