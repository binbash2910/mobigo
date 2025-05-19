import dayjs from 'dayjs/esm';
import { IRide } from 'app/entities/ride/ride.model';
import { IPeople } from 'app/entities/people/people.model';
import { BookingStatusEnum } from 'app/entities/enumerations/booking-status-enum.model';

export interface IBooking {
  id: number;
  nbPlacesReservees?: number | null;
  montantTotal?: number | null;
  dateReservation?: dayjs.Dayjs | null;
  statut?: keyof typeof BookingStatusEnum | null;
  trajet?: IRide | null;
  passager?: IPeople | null;
}

export type NewBooking = Omit<IBooking, 'id'> & { id: null };
