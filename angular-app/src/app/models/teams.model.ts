export interface TeamsModel {
  idTeam: string;
  teamName: string;
  driver: { driverId: string; driverName: string };
  electrician: { electricianId: string; electricianName: string };
  UFName: string;
  cityName: string;
  regionName: string;
  plate: string;
  depositName: string
  sel: boolean;
}
