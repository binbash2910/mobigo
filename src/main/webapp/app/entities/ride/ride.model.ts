import dayjs from 'dayjs/esm';
import { IVehicle } from 'app/entities/vehicle/vehicle.model';
import { RideStatusEnum } from 'app/entities/enumerations/ride-status-enum.model';

export interface IRide {
  id: number;
  villeDepart?: string | null;
  villeArrivee?: string | null;
  dateDepart?: dayjs.Dayjs | null;
  dateArrivee?: dayjs.Dayjs | null;
  heureDepart?: string | null;
  heureArrivee?: string | null;
  minuteDepart?: string | null;
  minuteArrivee?: string | null;
  prixParPlace?: number | null;
  nbrePlaceDisponible?: number | null;
  statut?: keyof typeof RideStatusEnum | null;
  vehicule?: IVehicle | null;
}

export type NewRide = Omit<IRide, 'id'> & { id: null };
