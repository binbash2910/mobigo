import dayjs from 'dayjs/esm';
import { IRide } from 'app/entities/ride/ride.model';
import { IPeople } from 'app/entities/people/people.model';

export interface IRating {
  id: number;
  note?: number | null;
  commentaire?: string | null;
  ratingDate?: dayjs.Dayjs | null;
  trajet?: IRide | null;
  passager?: IPeople | null;
  conducteur?: IPeople | null;
}

export type NewRating = Omit<IRating, 'id'> & { id: null };
