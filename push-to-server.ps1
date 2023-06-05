get-content .env | foreach {
  $name, $value = $_.split('=');
  set-content env:\$name $value;
};

$client = New-Object System.Net.WebClient;
$client.Credentials = New-Object System.Net.NetworkCredential($Env:FtpUsername, $Env:FtpPassword);
$client.UploadFile($Env:Destination, $Env:Source);