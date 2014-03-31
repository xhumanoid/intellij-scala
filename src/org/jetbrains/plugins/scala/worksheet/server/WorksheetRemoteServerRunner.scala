package org.jetbrains.plugins.scala
package worksheet.server

import org.jetbrains.jps.incremental.scala.remote.RemoteResourceOwner
import java.net.{UnknownHostException, ConnectException, InetAddress}
import org.jetbrains.plugins.scala.compiler.ScalaApplicationSettings
import org.jetbrains.jps.incremental.scala.Client
import org.jetbrains.plugins.scala.components.WorksheetProcess
import com.intellij.util.Base64Converter
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetCompiler
import com.intellij.openapi.project.Project

/**
 * User: Dmitry Naydanov
 * Date: 2/24/14
 */
class WorksheetRemoteServerRunner(project: Project) extends RemoteResourceOwner {
  protected val address = InetAddress.getByName(null)

  protected val port =
    try
      Integer parseInt ScalaApplicationSettings.getInstance().COMPILE_SERVER_PORT
    catch {
      case e: NumberFormatException =>
        throw new IllegalArgumentException("Bad port: " + ScalaApplicationSettings.getInstance().COMPILE_SERVER_PORT , e)
    }
  
  def run(arguments: Seq[String], client: Client) = new WorksheetProcess {
    val COUNT = 5

    var callback: Option[() => Unit] = None

    override def setTerminationCallback(callback: => Unit) {
      this.callback = Some(() => callback)
    }

    override def run() {
      val encodedArgs = arguments map (s => Base64Converter.encode(s getBytes "UTF-8"))

      try
        for (i <- 1 to (COUNT - 1)) {
          try {
            send(serverAlias, encodedArgs, client)
            return
          } catch {
            case _: ConnectException =>
          }

          send(serverAlias, encodedArgs, client)
        }
      catch {
        case e: ConnectException =>
          val message = "Cannot connect to compile server at %s:%s".format(address.toString, port)
          client.error(message)
        case e: UnknownHostException =>
          val message = "Unknown IP address of compile server host: " + address.toString
          client.error(message)
      } finally callback.foreach(a => a())
    }

    override def stop() {
      WorksheetCompiler.ensureNotRunning(project)
    }
  }
}
