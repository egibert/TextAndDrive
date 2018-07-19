using System.Text;
using Microsoft.Azure.Devices;
using Newtonsoft.Json.Linq;

public static void Run(string myQueueItem, TraceWriter log)
{
    JObject json = JObject.Parse(myQueueItem);
    string id = json["deviceId"].ToString(Newtonsoft.Json.Formatting.None);

    ServiceClient serviceClient = ServiceClient.CreateFromConnectionString("HostName=TextAndDriveHub.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=IhX0uflEUDcNFwsap4EydAp7liRjFpNdRf2MZjxWVT4=");
    var commandMessage = new Message(Encoding.ASCII.GetBytes(myQueueItem));
    serviceClient.SendAsync(id, commandMessage);
}
