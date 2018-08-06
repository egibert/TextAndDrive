using System;
using System.Text.RegularExpressions;
using Newtonsoft.Json.Linq;
using System.Text;
using System.IO; 
using System.Diagnostics; 

public static void Run(string myIoTHubMessage, TraceWriter log, out string outputQueueItem)
{
    JObject json = JObject.Parse(myIoTHubMessage);
    string id = json["deviceId"].ToString(Newtonsoft.Json.Formatting.None);
    string cleanID = id.Replace("\"", "");
    log.Info($"ID: {cleanID} ");    

    string state = json["state"].ToString(Newtonsoft.Json.Formatting.None);
    string cleanState = state.Replace("\"", "");
    log.Info($"State: {cleanState} ");

    string img = json["img"].ToString(Newtonsoft.Json.Formatting.None);
    string cleanIMG = img.Replace("\"", string.Empty).Replace('-', '+').Replace('_', '/');
    log.Info($"Img: {cleanIMG} ");

    //log.Info($"Recived: {clean}");
    //log.Info($"Expectd: {data} ");
    var base64Data = Regex.Match(cleanIMG, @"data:image/(?<type>.+?),(?<data>.+)").Groups["data"].Value;
    var binData = Convert.FromBase64String(base64Data);
    long milliseconds = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;
    string fileName = cleanID+ "_" + cleanState +"_"+ milliseconds.ToString() + ".jpeg";
    string path = @"D:\home";
    
    fileName = Path.Combine(path, fileName);
    File.WriteAllBytes(fileName, binData);

    outputQueueItem = fileName;
}
