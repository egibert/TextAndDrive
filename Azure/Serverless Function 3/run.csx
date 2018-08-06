using System.Text;
using Microsoft.Azure.Devices;
using Newtonsoft.Json.Linq;

public static void Run(string myQueueItem, out object outputDocument, TraceWriter log)
{
    JObject json = JObject.Parse(myQueueItem);
    string id = json["deviceID"].ToString(Newtonsoft.Json.Formatting.None);
    string cleanID = id.Replace("\"", "");
    
    string State = json["state"].ToString(Newtonsoft.Json.Formatting.None);
    string cleanState = State.Replace("\"", "");

    ServiceClient serviceClient = ServiceClient.CreateFromConnectionString("HostName=TextAndDriveHub.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=IhX0uflEUDcNFwsap4EydAp7liRjFpNdRf2MZjxWVT4=");
    var commandMessage = new Message(Encoding.ASCII.GetBytes(myQueueItem));
    serviceClient.SendAsync(cleanID, commandMessage);

    DateTime thisDay = DateTime.Today;

    outputDocument = new {
        state = cleanState,
        date = thisDay.ToString("D"),
    };   
}
