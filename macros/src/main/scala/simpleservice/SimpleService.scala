/**
 * Author: laurencer (Laurence Rouesnel)
 * Date: 8/09/13
 */
package simpleservice

object SimpleService {
  type Id = Long
  type Path = String
  type Result = Long
  type SessionId = Long
  type SimpleError = List[String]

  type SimpleObjectBuilder[T] =
    (T) => (SimpleService, SessionId, Path) => Path
}

trait SimpleService {
  import SimpleService._

  def createSession() : SessionId

  def createBusinessObject(sessionId: SessionId,
                           objType: String,
                           path: Path = "/") : Id

  def setBusinessObjectValue(sessionId: SessionId,
                             instance: Id,
                             name: String,
                             value: String) : Result

  def getPath(sessionId: SessionId, instance: Id) : Path

  def destroySession(sessionId: SessionId) : Result
}